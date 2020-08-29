properties([
    parameters([
        string(defaultValue: '', description: 'Please enter VM IP', name: 'nodeIP', trim: true)
        ])
    ])
if (nodeIP.length() > 6) {
    node {
        stage('Pull Repo') {
            git branch: 'master', changelog: false, poll: false, url: 'https://github.com/ikambarov/spring-petclinic.git'
        }
        withCredentials([sshUserPrivateKey(credentialsId: 'jenkins-master-ssh-key', keyFileVariable: 'SSHKEY', passphraseVariable: '', usernameVariable: 'SSHUSERNAME')]) {
            stage("Install Apache"){
                sh '''
                    export ANSIBLE_HOST_KEY_CHECKING=False
                    ansible-galaxy -i "157.245.84.190," --private-key $SSHKEY install gantsign.maven
                    '''
            stage("Install Maven"){
                sh 'ssh -o StrictHostKeyChecking=no -i $SSHKEY $SSHUSERNAME@${nodeIP} ansible-galaxy install gantsign.maven'
            }
            stage("Install Java"){
                sh 'ssh -o StrictHostKeyChecking=no -i $SSHKEY $SSHUSERNAME@${nodeIP} ansible-galaxy install gantsign.java'
            }
        }  
    }

else {
    error 'Please enter valid IP address'
}