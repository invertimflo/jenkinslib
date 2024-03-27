package org.cicd.devops

import org.cicd.enums.*
import org.cicd.utils.utils


/**
 * 检出项目代码
 * @param srcUrl
 * @param branchName
 * @return
 */
def checkOutCode(Map params) {
    def utils = new utils()
    utils.printMessage("获取代码", "green")
    checkOut(params.get("src_url"), params.get("branch_name"))
    //初始化包信息
    getGav(params)
}
/**
 * 检出代码
 * @param srcUrl
 * @param branchName
 * @return
 */
def checkOut(srcUrl, branchName) {
    checkout([$class                           : 'GitSCM', branches: [[name: "${branchName}"]],
              doGenerateSubmoduleConfigurations: false,
              extensions                       : [],
              submoduleCfg                     : [],
              userRemoteConfigs                : [[credentialsId: 'bitbucket_secret', url: "${srcUrl}"]]])
}



/**
 * 取包的的坐标信息
 */
def getGav(Map params) {
    switch (params.get('build_type')) {
        case "mvn":
            println("获取信息")
            def projectInfo = readMavenPom file: 'pom.xml'
            params.put('app_version', "${projectInfo.version}")
            //  对于maven多模块的解析方案，获取需要发布的包的artifact方案
//            def projectId = "${projectInfo.properties["deploy.moudle.name"]}"
            def projectId = params.get('artifact_id')
            if ("null" == projectId || "" == projectId) {
                println("maven单模块")
                projectId = "${projectInfo.artifactId}"
            }
            println(projectInfo)
            println("${projectInfo.properties["project.name"]}")
            params.put('app_code', "${projectInfo.properties["project.name"]}")
            params.put('group_id', "${projectInfo.groupId}")
            params.put('artifact_id', projectId)
            break
        case "npm":
            def projectInfo = readJSON file: 'package.json'
            params.put('app_version', "${projectInfo.package_version}")
            params.put('app_code', "${projectInfo.app_code}")
            params.put('group_id', "${projectInfo.group_id}")
            params.put('artifact_id', "${projectInfo.name}")
            break
    }
    if (null == params.get("app_code") || null == params.get("group_id") || null == params.get("artifact_id") || null == params.get("app_version")) {
        error '项目信息不全，请先补全'
    }
    println(params)
}

