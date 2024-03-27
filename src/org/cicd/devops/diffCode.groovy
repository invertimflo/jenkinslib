package org.cicd.devops

import org.cicd.utils.utils

/**
 * 获取差异代码
 * @param params
 * @return
 */
def getCodeDiff(map) {
    def utils = new utils()
    def reqUrl = "api/code/diff/git/list?gitUrl=${map["gitUrl"]}&baseVersion=${map["baseVersion"]}&nowVersion=${map["nowVersion"]}"
    def reqType = "GET"
    def response = utils.HttpReq(map.get("code_diff_domain"), reqType, reqUrl, "", "")
    def resMap = readJSON text: """${response.content}"""
    if (resMap["code"].toString() != "10000") {
        error "'获取差异代码失败！"
    }
    //这里可以讲json存储到文件
    return resMap["data"]
}


/**
 * 执行jacoco结果集
 * @param map
 */
def jacocoExec(map) {
    def codeDiffJson = getCodeDiff(map)
    sh """
        java -jar jacococli.jar report ${map["execPath"]} \
        --classfiles ${map["classfiles"]} --sourcefiles ${map["sourcefiles"]} --html ${map["htmlPath"]} \
        --xml ${map["xmlPath"]} --diffCode $codeDiffJson \
        --encoding utf8
    """
    //这里可以传jsonpath  --diffCodeFiles /usr/local/a.json
}
