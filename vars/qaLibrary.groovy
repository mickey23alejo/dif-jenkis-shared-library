def call(body) {
    // evaluate the body block, and collect configuration into the object
    def config= [:]
    body.resolveStrategy = Closure.DELEGATE_FIRST
    body.delegate = config
    body()

    pipeline {
        agent {
		  kubernetes {
			label 'algo-slave'
			yaml """
              apiVersion: v1
              kind: Pod
              metadata:
                name: privileged
                labels:
                  jenkins-agent: algo-jnlp-slave
                  jenkins/algo-slave: true
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
                      value: --insecure-registry=http://10.128.3.113:8083
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
                - name: wget
                  image: mwendler/wget
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
            // stage('docker build') {
            //     // steps {
            //     //     container('buildah'){
            //     //             //sh "podman build -f Dockerfile -t qa-'${config.name}'-image:v1.0.$BUILD_NUMBER ."
            //     //             //sh "buildah bud -f Dockerfile -t qa-'${config.name}'-image:v1.0.$BUILD_NUMBER ."
            //     //             sh "whoami"
            //     //     }
            //     // }
            //     steps {
            //         container('docker'){
            //                 // sh "docker version"
            //                 // sh "whoami"
            //                 // sh "docker ps"
            //                 sh "docker build -f Dockerfile -t qa-'${config.name}'-image:v1.0.$BUILD_NUMBER ."
            //                 //sh "docker login -u admin --password n2oxM-poryD-ew92Y-a2tFn 10.100.43.10:8083"
            //                 //sh "docker login 10.128.3.113:8083"
            //                 //sh "docker login -u admin --password $NEXUS_PASSWORD http://10.128.3.113:8083"
            //                 //sh "SUCCESS_BUILD=`wget -qO- --user 'kubeadmin' --password 'n2oxM-poryD-ew92Y-a2tFn' --auth-no-challenge https://jenkins-jenkins.apps.ocp4mqa.grupodifare.com/job/EjemploMultibranch2/lastSuccessfulBuild/buildNumber`"
            //                 //sh "echo SUCCESS_BUILD = $SUCCESS_BUILD - 1"
            //                 //sh "docker tag qa-'${config.name}'-image:v1.0.$BUILD_NUMBER 10.128.3.113:8083/qa-'${config.name}'-image:v1.0.$BUILD_NUMBER"
            //         }
            //     }
            // }
            stage('wget'){
              steps{
                container('wget'){
                  script{
                    'SUCCESS_BUILD=`https://jenkins-jenkins.apps.ocp4mqa.grupodifare.com/job/MicroservicesQA/job/Test.CargaMasiva2/job/%2500/lastSuccessfulBuild/buildNumber` echo $SUCCESS_BUILD'
                  }
                  
                }
              }
            }
             // Validado
            // stage('oc-client') {
            //     steps {
            //         container('oc-client'){
            //                 sh "oc login $OC_URL --insecure-skip-tls-verify=true --username=$OC_USER --password=$OC_PASS"
            //                 sh "oc apply -f $WORKSPACE/jenkins/deployment.yml -n test1"
            //                 sh "oc set image deployment.v1.apps/deployment-test-ci nginx=nginx:alpine -n test1 --record=true"
            //         }
            //     }
            // }
        }
      }
}