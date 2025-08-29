#!/usr/bin/env groovy

/**
 * Update Kubernetes manifests with new image tags and push changes
 */
def call(Map config = [:]) {
    def imageTag     = config.imageTag ?: error("Image tag is required")
    def manifestsPath = config.manifestsPath ?: 'kubernetes'
    def gitCredentials = config.gitCredentials ?: 'github-credentials'
    def gitUserName  = config.gitUserName ?: 'Jenkins CI'
    def gitUserEmail = config.gitUserEmail ?: 'jenkins@example.com'

    // Detect branch from Jenkins env, fallback to master
    def gitBranch = env.BRANCH_NAME ?: 'master'

    echo "Updating Kubernetes manifests with image tag: ${imageTag} on branch: ${gitBranch}"

    withCredentials([usernamePassword(
        credentialsId: gitCredentials,
        usernameVariable: 'GIT_USERNAME',
        passwordVariable: 'GIT_PASSWORD'
    )]) {
        sh """
            git config user.name "${gitUserName}"
            git config user.email "${gitUserEmail}"

            # Update main application deployment
            sed -i "s|image: akshayaws99/easyshop-app:.*|image: akshayaws99/easyshop-app:${imageTag}|g" ${manifestsPath}/08-easyshop-deployment.yaml

            # Update migration job if it exists
            if [ -f "${manifestsPath}/12-migration-job.yaml" ]; then
                sed -i "s|image: akshayaws99/easyshop-migration:.*|image: akshayaws99/easyshop-migration:${imageTag}|g" ${manifestsPath}/12-migration-job.yaml
            fi

            # Ensure ingress has correct domain
            if [ -f "${manifestsPath}/10-ingress.yaml" ]; then
                sed -i "s|host: .*|host: easyshop.letsdeployit.com|g" ${manifestsPath}/10-ingress.yaml
            fi

            # Check for changes
            if git diff --quiet; then
                echo "No manifest changes to commit"
            else
                git add ${manifestsPath}/*.yaml
                git commit -m "Update image tags to ${imageTag} [ci skip]"

                # Set remote with credentials (patched repo URL)
                git remote set-url origin https://${GIT_USERNAME}:${GIT_PASSWORD}@github.com/akshayaws99/tws-e-commerce-app_hackathon.git

                # Push to correct branch
                git push origin HEAD:${gitBranch}
            fi
        """
    }
}
