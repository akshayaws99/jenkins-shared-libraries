#!/usr/bin/env groovy

def call(Map config = [:]) {
    def imageTag       = config.imageTag     ?: error("Image tag is required")
    def manifestsPath  = config.manifestsPath ?: 'kubernetes'
    def gitCredentials = config.gitCredentials ?: 'github-credentials'
    def gitUserName    = config.gitUserName  ?: 'Jenkins CI'
    def gitUserEmail   = config.gitUserEmail ?: 'jenkins@example.com'
    def dryRun         = config.dryRun ?: false

    def gitBranch = env.BRANCH_NAME ?: 'master'

    echo "🔧 Updating Kubernetes manifests with image tag: ${imageTag} on branch: ${gitBranch}"

    withCredentials([usernamePassword(
        credentialsId: gitCredentials,
        usernameVariable: 'GIT_USERNAME',
        passwordVariable: 'GIT_PASSWORD'
    )]) {
        sh """
            set -e
            git config user.name "${gitUserName}"
            git config user.email "${gitUserEmail}"

            # Ensure clean workspace
            git clean -fd
            git reset --hard

            # Update application deployment
            sed -i "s|image: akshayaws99/easyshop-app:.*|image: akshayaws99/easyshop-app:${imageTag}|g" ${manifestsPath}/08-easyshop-deployment.yaml

            # Update migration job if exists
            if [ -f "${manifestsPath}/12-migration-job.yaml" ]; then
                sed -i "s|image: akshayaws99/easyshop-migration:.*|image: akshayaws99/easyshop-migration:${imageTag}|g" ${manifestsPath}/12-migration-job.yaml
            fi

            # Update ingress host if exists
            if [ -f "${manifestsPath}/10-ingress.yaml" ]; then
                sed -i "s|host: .*|host: easyshop.letsdeployit.com|g" ${manifestsPath}/10-ingress.yaml
            fi

            # Commit only if there are changes
            if git diff --quiet; then
                echo "✅ No manifest changes detected"
            else
                git add ${manifestsPath}/*.yaml
                git commit -m "Update image tags to ${imageTag} [ci skip]"

                # Authenticated remote
                git remote set-url origin https://${GIT_USERNAME}:${GIT_PASSWORD}@github.com/akshayaws99/tws-e-commerce-app_hackathon.git
        """

        if (!dryRun) {
            sh """
                git push origin HEAD:${gitBranch} || echo "⚠️ Git push failed/skipped"
            """
        } else {
            echo "💡 Dry run enabled: skipping git push"
        }
    }
}
