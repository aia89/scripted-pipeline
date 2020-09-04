properties([
    parameters([
        booleanParam(defaultValue: false, description: 'Do you want to run terrform destroy', name: 'terraform_destroy'),
        string(defaultValue: '', description: 'Provide SOURCE_PROJECT_NAME', name: 'SOURCE_PROJECT_NAME', trim: true)
    ])
])
def aws_region_var = ''
def environment = ''
if(params.SOURCE_PROJECT_NAME ==~ "dev.*"){
    aws_region_var = "us-east-1"
    environment = 'dev'
}
else if(params.SOURCE_PROJECT_NAME ==~ "qa.*"){
    aws_region_var = "us-east-2"
    environment = 'qa'
}
else if(params.SOURCE_PROJECT_NAME ==~ "master"){
    aws_region_var = "us-west-2"
    environment = 'prod'
}
else {
    error("SOURCE_PROJECT_NAME Name Doesnt Match RegEx")
}
def tf_vars = """
    s3_bucket = \"terraform-state-aipril-class-aia\"
    s3_folder_project = \"terraform\"
    s3_folder_region = \"us-east-1\"
    s3_folder_type = \"class\"
    s3_tfstate_file = \"infrastructure.tfstate\"
    environment = \"${environment}\"
    region      = \"${aws_region_var}\"
    public_key  = \"ssh-rsa AAAAB3NzaC1yc2EAAAADAQABAAABAQDO1KaBM9apTAiwJA7rnSlfCeHXRi7gGHFmQNOe0HFAaFvJNLZ+X+CD5nxjUBl3u7BGmODcgv3LSMAz8hIXbYvHCI33OY4LLmX6HIeHJebAJjmLpxAiQYjTV2s4b2XMyM1adK0ZVGWXECMOf7O+vIHlz66rmoARSIEjDF86/NImxWV81QFfZaasc4ccRAhSxaRDOB9HLjTrIZ6T2rcMzO6i3hZ2kPmf5Ua+QDvC1ShrQWxJO8TKTXTrKOMrsOJzGW64C10Pi2aray5abRRuvYlOGQhqj+QwmVYske7wV4x0/m4zECYyhJN/6i3ZhGj3q6Nt3qUlFfhWpfaG/Ak/fcNn admin@MacBook-Pro-2\"
    ami_name      = \"*\"
"""
node{
    stage("Pull Repo"){
        cleanWs()
        git url: 'https://github.com/aia89/terraform-ec2.git'
    }
    withCredentials([usernamePassword(credentialsId: 'jenkins-aws-access-key', passwordVariable: 'AWS_SECRET_ACCESS_KEY', usernameVariable: 'AWS_ACCESS_KEY_ID')]) {
        withEnv(["AWS_REGION=${aws_region_var}"]) {
            stage("Terrraform Init"){
                writeFile file: "${environment}.tfvars", text: "${tf_vars}"
                sh """
                    bash setenv.sh ${environment}.tfvars
                    terraform-0.13 init
                """
            }        
            if (terraform_destroy.toBoolean()) {
                stage("Terraform Destroy"){
                    sh """
                        terraform-0.13 destroy -var-file ${environment}.tfvars -auto-approve
                    """
                }
            }
            else {
                stage("Terraform Plan"){
                    sh """
                        terraform-0.13 plan -var-file ${environment}.tfvars
                    """
                }
            }
        }        
    }    
}