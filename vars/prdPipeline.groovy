def call(body) {
    def config= [:]
    body.resolveStrategy = Closure.DELEGATE_FIRST
    body.delegate = config
    body()

    pipeline {
        agent {
		  kubernetes {
			label 'produccion-slave'
			yaml """
              apiVersion: v1
              kind: Pod
              metadata:
                name: privileged
                labels:
                  jenkins-agent: produccion-jnlp-slave
                  jenkins/produccion-slave: true
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
                      value: --insecure-registry=https://dockerprd.grupodifare.com
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
                            sh "docker build -f Dockerfile -t prod-'${config.name}'-image:v1.0.$BUILD_NUMBER ."
                            sh "docker login -u admin --password $NEXUS_PASSWORD https://dockerprd.grupodifare.com"
                            sh "docker tag prod-'${config.name}'-image:v1.0.$BUILD_NUMBER dockerprd.grupodifare.com/prod-'${config.name}'-image:v1.0.$BUILD_NUMBER"
                            sh "docker push dockerprd.grupodifare.com/prod-'${config.name}'-image:v1.0.$BUILD_NUMBER"
                    }
                }
            }
        }
      }
}