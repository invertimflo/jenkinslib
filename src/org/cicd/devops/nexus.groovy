package org.cicd.devops

import org.cicd.utils.utils

/**
 * iqserver扫描
 * @return
 */
def iqServerScan(Map params) {
    def utils = new utils()
    utils.printMessage("包扫描", "green")
    def scanPatterns = ""

    if (params.get("build_type") == "mvn") {
        scanPatterns = "**/target/${params.get("artifact_id")}-${params.get("app_version")}.jar"
    } else {
        scanPatterns = "**/*.js"
    }
    catchError(buildResult: 'SUCCESS', message: '包扫描存在安全隐患') {
        dir(pwd()){
            nexusPolicyEvaluation iqApplication: manualApplication("${params.get("artifact_id")}-${params.get("app_version")}"), iqScanPatterns: [[scanPattern: "${scanPatterns}"]], iqStage: "${params.get("scan_stage")}"
        }

    }
}


/**
 * 构建包并上传至nexus
 * @param buildType
 * @return
 */
def build(Map params) {
    def utils = new utils()
    utils.printMessage("构建代码", "green")
    switch (params.get("build_type")) {
        case "mvn":
            //后端构建至nexus
            sh "${params.get("build_type")} deploy  -pl :${params.get("artifact_id")} -am -Dmaven.test.skip=true "
            //处理script脚本
            def shellPath = "${params.get("artifact_id")}/script/"
            uploadScript(shellPath, params.get("artifact_id"), params.get("group_id"), params.get("app_version"))
            break
        case "npm":
            npmBuild(params.get("env_info"), params.get("artifact_id"), params.get("group_id"), params.get("app_version"))
            //前端构建至nexus
            break
    }
}

def uploadScript(path, projectArtifact, projectGroupId, projectVersion) {
    def utils = new utils()
    //判断是否存在脚本
    if (!utils.checkFileExist("${path}*.sh")) {
        println("不存在shell脚本")
        return
    }
    //打包处理
    dealZipFile("${projectArtifact}-script-${projectVersion}.zip", path, "${projectArtifact}-script", "${projectGroupId}", "${projectVersion}")

}


/**
 * npm 打成zip包，vue不是这样打包的，私服管起来啊，没办法，屌丝码农没有话语权
 * @param env_info
 * @return
 */
def npmBuild(env_info, projectArtifact, projectGroupId, projectVersion) {
    //编译前端代码
    sh """
        npm install
        npm run ${env_info}
       """
    //打包处理
    dealZipFile("${projectArtifact}-${projectVersion}.zip", "dist/", "${projectArtifact}", "${projectGroupId}", "${projectVersion}")
}


/**
 * 处理zip打包
 * @param filePath
 * @param sourcePath
 * @param artifactId
 * @param groupId
 * @param version
 * @return
 */
def dealZipFile(filePath, sourcePath, artifactId, groupId, version) {
    def utils = new utils()
    utils.zipFile(sourcePath, filePath)
    //上传至nexus
    otherPublishToNexus(filePath, artifactId, groupId, version)
}


/**
 * 上传文件到nexus
 * @param filePath
 * @param artifactId
 * @param groupId
 * @param version
 * @return
 */
def otherPublishToNexus(filePath, artifactId, groupId, version) {
    nexusPublisher nexusInstanceId: 'Nexus', nexusRepositoryId: 'release',
            packages: [[$class: 'MavenPackage', mavenAssetList: [[classifier: '', extension: '', filePath: "${filePath}"]], mavenCoordinate: [artifactId: "${artifactId}", groupId: "${groupId}", packaging: 'zip', version: "${version}"]]], tagName: "${artifactId}-${version}"

}
