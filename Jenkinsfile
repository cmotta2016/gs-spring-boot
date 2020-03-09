timestamps {
    node('nodejs') {
        stage('Checkout'){
            checkout scm
            //checkout([$class: 'GitSCM', branches: [[name: '*/aks']], doGenerateSubmoduleConfigurations: false, extensions: [], submoduleCfg: [], userRemoteConfigs: [[url: 'https://github.com/cmotta2016/PipelineScriptsBackendProject.git']]])
        }
        stage('Compile with Azure Artifacts'){
            sh 'npm config set registry=https://pkgs.dev.azure.com/carlosmotta0608/cicd/_packaging/npm-feed/npm/registry/'
            sh 'npm config set always-auth=true'
            sh 'cp /opt/npm/.npmrc /home/jenkins/'
            sh 'npm install'
        }
        stage ('Test'){
            sh 'npm test'
        }
        stage ('Code Quality'){
            def sonar = load 'sonar.groovy'
            sonar.codeQuality()
        }
        stage('Quality Gate'){
            sleep(15)
            timeout(activity: true, time: 15, unit: 'SECONDS') {
                def qg = waitForQualityGate()
                if (qg.status.toUpperCase() == 'ERROR') {
                    error "Pipeline aborted due to quality gate failure: ${qg.status}"
                }
            }
        }
        stage('Build with S2I'){
            sh 's2i build . cmotta2016/nodejs-10-bases2i:latest cmotta2016.azurecr.io/node-app:${BUILD_NUMBER} --loglevel 5 --network host --env npm_config_registry=https://pkgs.dev.azure.com/carlosmotta0608/cicd/_packaging/npm-feed/npm/registry/ --inject /opt/npm:/opt/app-root/src'
        }
        stage('Push Image to ACR'){
            withCredentials([usernamePassword(credentialsId: 'acr-credentials', passwordVariable: 'DOCKER_PASSWORD', usernameVariable: 'DOCKER_USERNAME')]) {
            sh '''
            docker login -u "$DOCKER_USERNAME" -p "$DOCKER_PASSWORD" cmotta2016.azurecr.io
            docker tag cmotta2016.azurecr.io/node-app:${BUILD_NUMBER} cmotta2016.azurecr.io/node-app:latest
            docker push cmotta2016.azurecr.io/node-app:${BUILD_NUMBER}
            docker push cmotta2016.azurecr.io/node-app:latest
            docker rmi -f cmotta2016.azurecr.io/node-app:${BUILD_NUMBER} cmotta2016.azurecr.io/node-app:latest
            '''
            }
        }
        stage('Deploy QA'){
            def deployment = sh(script: "kubectl get deployment nodejs -n nodejs-qa -o jsonpath='{ .metadata.name }' --ignore-not-found", returnStdout: true).trim()
            if (deployment == "nodejs") {
                sh 'kubectl set image deployment/nodejs nodejs=cmotta2016.azurecr.io/node-app:${BUILD_NUMBER} --record -n nodejs-qa'
            }
            else {
                sh 'kubectl create -f aks-nodejs.yaml --validate=false -l env!=hml -n nodejs-qa'
                sh 'kubectl set image deployment/nodejs nodejs=cmotta2016.azurecr.io/node-app:${BUILD_NUMBER} --record -n nodejs-qa'
            }
            //sh 'kubectl wait --for=condition=Ready deployment/nodejs -n nodejs-qa'
            sh 'kubectl rollout status deployment.apps/nodejs -n nodejs-qa'
        }
        /*stage('Promote to HML'){
            //routeHost = sh(script: "kubectl get ingress nodejs -n nodejs-qa -o jsonpath='{ .spec.rules[0].host }'", returnStdout: true).trim()
            input message: "Promote to HML. Approve?", id: "approval"
        }*/
        stage('Test Deployment'){
            routeHost = sh(script: "kubectl get ingress nodejs -n nodejs-qa -o jsonpath='{ .spec.rules[0].host }'", returnStdout: true).trim()
            input message: "Test deployment: http://${routeHost}. Approve?", id: "approval"
        }
        /*stage('Deploy HML'){
            def deployment = sh(script: "kubectl get deployment nodejs -n nodejs-hml -o jsonpath='{ .metadata.name }' --ignore-not-found", returnStdout: true).trim()
            if (deployment == "nodejs") {
                sh 'kubectl set image deployment/nodejs nodejs=cmotta2016.azurecr.io/node-app:${BUILD_NUMBER} --record -n nodejs-hml'
            }
            else {
                sh "kubectl create -f aks-nodejs.yaml -n nodejs-hml --validate=false --dry-run -o yaml | sed 's/qa/hml/g' | kubectl apply --validate=false -f -"
                sh 'kubectl set image deployment/nodejs nodejs=cmotta2016.azurecr.io/node-app:${BUILD_NUMBER} --record -n nodejs-hml'
            }
            //sh 'kubectl wait --for=condition=Available deployment/nodejs -n nodejs --timeout=90s'
            sh 'kubectl rollout status deployment.apps/nodejs -n nodejs-hml'
        }
        stage('Test Deployment'){
            routeHost = sh(script: "kubectl get ingress nodejs -n nodejs-qa -o jsonpath='{ .spec.rules[0].host }'", returnStdout: true).trim()
            input message: "Test deployment: http://${routeHost}. Approve?", id: "approval"
        }*/
    }
}