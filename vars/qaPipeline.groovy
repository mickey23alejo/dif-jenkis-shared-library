def call(body) {
    def config= [:]
    body.resolveStrategy = Closure.DELEGATE_FIRST
    body.delegate = config
    body()

    pipeline {
        agent {
		  kubernetes {
			label 'qa-slave'
			yaml """
              apiVersion: v1
              kind: Pod
              metadata:
                name: privileged
                labels:
                  jenkins-agent: qa-jnlp-slave
                  jenkins/qa-slave: true
              spec:
                serviceAccount: cd-jenkins
                containers:
                - name: docker
                  image: docker:18.09.7
                  command: ['docker', 'run', '-p', '80:80', 'httpd:latest'] 
                  resources: 
                    requests: 
                        cpu: 10m 
                        memory: 256Mi 
                  env: 
                    - name: DOCKER_HOST 
                      value: tcp://localhost:2375
                    - name: DOCKER_OPTS
                      value: --insecure-registry=https://dockernp.grupodifare.com
                - name: dind-daemon 
                  image: docker:18.09.7-dind 
                  resources: 
                     requests: 
                      cpu: 20m 
                      memory: 512Mi
                  securityContext:
                    privileged: true
                  tty: true
                - name: oc-client
                  image: widerin/openshift-cli
                  command:
                  - cat
                  tty: true
                imagePullSecrets:
                  - name: nexus-secret
                volumeMounts:
                  - name: docker-graph-storage 
                    mountPath: /var/lib/docker
                """
            }
        }
        stages {
            stage('docker build') {
                steps {
                    container('docker'){
                            sh "docker build -f Dockerfile -t qa-'${config.name}'-image:v1.0.$BUILD_NUMBER ."
                            sh "docker login -u admin --password $NEXUS_PASSWORD https://dockernp.grupodifare.com"
                            sh "docker tag qa-'${config.name}'-image:v1.0.$BUILD_NUMBER dockernp.grupodifare.com/qa-'${config.name}'-image:v1.0.$BUILD_NUMBER"
                            sh "docker push dockernp.grupodifare.com/qa-'${config.name}'-image:v1.0.$BUILD_NUMBER"
                    }
                }
            }
            stage('oc-client') {
                steps {
                    container('oc-client'){
                            sh "oc login $OC_URL --insecure-skip-tls-verify=true --username=$OC_USER --password=$OC_PASS"
                            sh "oc apply -f $WORKSPACE/CI/QA/openshift/deployment.yml -n ${config.namespace}"
                            sh "oc set image deployment.v1.apps/${config.name}-deploy ${config.name}=dockernp.grupodifare.com/qa-'${config.name}'-image:v1.0.$BUILD_NUMBER -n ${config.namespace} --record=true"
                    }
                }
            }
        }
      }
}
