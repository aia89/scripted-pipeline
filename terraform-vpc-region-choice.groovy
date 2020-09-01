properties([
    parameters([
        booleanParam(defaultValue: true, description: 'Do you want to run terrform apply', name: 'terraform_apply'),
        booleanParam(defaultValue: false, description: 'Do you want to run terrform destroy', name: 'terraform_destroy'),
        choice(choices: ['dev', 'qa', 'prod'], description: '', name: 'environment')
    ])
])
def aws_region_var = ''
if(params.environment == "dev"){
    aws_region_var = "us-east-1"
}
else if(params.environment == "qa"){
    aws_region_var = "us-east-2"
}
else if(params.environment == "prod"){
    aws_region_var = "us-west-2"
}
node{
    stage("Pull Repo"){
        cleanWs()
        git url: 'https://github.com/ikambarov/terraform-vpc.git'
    }
    withCredentials([usernamePassword(credentialsId: 'jenkins-aws-access-key', passwordVariable: 'AWS_SECRET_ACCESS_KEY', usernameVariable: 'AWS_ACCESS_KEY_ID')]) {
        stage("Terrraform Init"){
            sh """
                bash setenv.sh ${environment}.tfvars
                export AWS_REGION=${aws_region_var}
                echo \$AWS_REGION
                terraform-0.13 init
                terraform-0.13 plan -var-file ${environment}.tfvars
            """
        }        
        if (terraform_apply.toBoolean()) {
            stage("Terraform Apply"){
                sh """
                    export AWS_REGION=${aws_region_var}
                    terraform-0.13 apply -var-file ${environment}.tfvars -auto-approve
                """
            }
        }
        else if (terraform_destroy.toBoolean()) {
            stage("Terraform Destroy"){
                sh """
                    export AWS_REGION=${aws_region_var}
                    terraform-0.13 destroy -var-file ${environment}.tfvars -auto-approve
                """
            }
        }
        else {
            stage("Uknown"){
                sh """
                    echo "Choose either apply or destroy"
                """
            }
        }
    }    
}





