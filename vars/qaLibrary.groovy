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
                  image: docker
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
            // stage('Docker build') {
            //     steps {
            //         container('docker'){
            //                 sh "cd $WORKSPACE"
            //                 sh "docker build -f Dockerfile -t qa-'${config.name}'-image:v1.0.$BUILD_NUMBER ."
            //         }
            //     }
            // }
            
            //stage('Connect to nexus'){
            //   steps{
            //     sh 'echo $NEXUS_PASSWORD | login -u $NEXUS_USER --password-stdin 10.100.43.10:8082'
            //     sh 'echo $NEXUS_PASSWORD | login -u $NEXUS_USER --password-stdin 10.100.43.10:8083'
            //   }
            // }

            stage('oc-client') {
                steps {
                    container('oc-client'){
                            sh "oc login $OC_URL --insecure-skip-tls-verify=true --username=$OC_USER --password=$OC_PASS"
                            sh "oc apply -f $WORKSPACE/jenkins/deployment.yml -n test1"
                            sh "oc set image deployment.v1.apps/deployment-test-ci nginx=nginx:alpine -n test1 --record=true"
                            
                    }
                }
            }
        }
      }
}