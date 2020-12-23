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
                - name: buildah
                  image: buildah/buildah
                  command: 
                  - cat
                  tty: true
                - name: oc-client
                  image: widerin/openshift-cli
                  command:
                  - cat
                  tty: true
                """
            }
        }
        stages {
            stage('Buildah build') {
                steps {
                    container('buildah'){
                            sh "cd $WORKSPACE"
                            sh "podman build -f Dockerfile -t qa-'${config.name}'-image:v1.0.$BUILD_NUMBER ."
                    }
                }
            }
            
            // Validado
            // stage('oc-client') {
            //     steps {
            //         container('oc-client'){
            //                 sh "oc login $OC_URL --insecure-skip-tls-verify=true --username=$OC_USER --password=$OC_PASS"
            //                 sh "oc apply -f $WORKSPACE/jenkins/deployment.yml -n test1"
            //                 sh "oc set image deployment.v1.apps/deployment-test-ci nginx=nginx -n test1 --record=true"
                            
            //         }
            //     }
            // }
        }
      }
}