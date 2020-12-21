def call(body) {
    // evaluate the body block, and collect configuration into the object
    def config= [:]
    body.resolveStrategy = Closure.DELEGATE_FIRST
    body.delegate = config
    body()

    pipeline {
        agent { label 'first_slave' }
        stages {
          stage('Docker build') {
            steps {
                sh 'cd $WORKSPACE'
                sh "/usr/bin/docker build -f Dockerfile -t qa-'${config.name}'-image:v1.0.$BUILD_NUMBER ."
            }        
          }
          stage('Push to Nexus'){
            steps{
              sh 'echo "$NEXUS_PASSWORD" | /usr/bin/docker login -u $NEXUS_USER --password-stdin 192.3.50.170:8082'
              sh 'echo "$NEXUS_PASSWORD" | /usr/bin/docker login -u $NEXUS_USER --password-stdin 192.3.50.170:8083'
              sh "/usr/bin/docker tag qa-'${config.name}'-image:v1.0.$BUILD_NUMBER 192.3.50.170:8083/qa-'${config.name}'-image:v1.0.$BUILD_NUMBER"
              sh "/usr/bin/docker push 192.3.50.170:8083/qa-'${config.name}'-image:v1.0.$BUILD_NUMBER"
            }
          }
          stage('SSH Conection') {
            steps{
              sh "ssh root@192.3.50.101 'kubectl apply -f ${config.urlYaml} && kubectl set image deployment.v1.apps/${config.name}-deploy ${config.name}=192.3.50.170:8083/qa-${config.name}-image:v1.0.$BUILD_NUMBER -n kube-${config.namespace}  --record=true'"
            }
          } 
        }    
    }
}