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
            stage('Docker build') {
                steps {
                    container('docker'){
                            // sh "service docker start"
                            sh "docker build -f Dockerfile -t qa-'${config.name}'-image:v1.0.$BUILD_NUMBER ."
                    }
                }
            }
            // stage('oc-client') {
            //     steps {
            //         container('oc-client'){
            //                 sh "oc login https://api.ocp4mqa.grupodifare.com:6443 --insecure-skip-tls-verify=true --username='kubeadmin' --password='n2oxM-poryD-ew92Y-a2tFn'"
            //         }
            //     }
            // }
        }
}


}