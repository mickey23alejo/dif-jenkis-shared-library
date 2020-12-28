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
                labels:
                  jenkins-agent: algo-jnlp-slave
                  jenkins/algo-slave: true
              spec:
                serviceAccount: cd-jenkins
                containers:
                - name: docker
                  image: docker:18.06.0-ce-git
                  command:
                  - cat
                  tty: true
                - name: oc-client
                  image: widerin/openshift-cli
                  command:
                  - cat
                  tty: true
                - name: buildah
                  image: buildah/buildah
                  command:
                  - cat
                  tty: true
                """
            }
        }
        stages {
            stage('docker build') {
                // steps {
                //     container('buildah'){
                //             //sh "podman build -f Dockerfile -t qa-'${config.name}'-image:v1.0.$BUILD_NUMBER ."
                //             //sh "buildah bud -f Dockerfile -t qa-'${config.name}'-image:v1.0.$BUILD_NUMBER ."
                //             sh "whoami"
                //     }
                // }
                steps {
                    container('docker'){
                            sh "whoami"
                            sh "docker ps"
                            //sh "docker build -f Dockerfile -t qa-'${config.name}'-image:v1.0.$BUILD_NUMBER ."
                            //sh "systemctl start docker"
                            //sh "systemctl status docker"
                            // sh "systemctl daemon-reload"

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