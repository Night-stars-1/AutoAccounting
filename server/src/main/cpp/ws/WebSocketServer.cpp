//
// Created by Ankio on 2024/5/14.
//

#include <cstdio>
#include <unistd.h>
#include <execinfo.h>
#include "WebSocketServer.h"
#include "../jsoncpp/include/json/value.h"
#include "../jsoncpp/include/json/reader.h"
#include "../db/DbManager.h"
#include "../common.h"
#include "../base64/include/base64.hpp"
#include <random>
#include <sys/stat.h>
std::string WebSocketServer::version;
WebSocketServer::WebSocketServer(int port) {
    initToken();
    version = getVersion();
    struct ws_server wsServer{};
    wsServer.host = "0.0.0.0";
    wsServer.port = static_cast<uint16_t>(port);
    wsServer.thread_loop = 0;
    wsServer.timeout_ms = 1000;
    wsServer.evs.onopen = &WebSocketServer::onOpen;
    wsServer.evs.onclose = &WebSocketServer::onClose;
    wsServer.evs.onmessage = &WebSocketServer::onMessage;

    ws_socket(&wsServer);
}

std::string WebSocketServer::getVersion() {
    FILE *file = fopen("version.txt", "r");
    if (file == nullptr) {
        file = fopen("version.txt", "w");
        fprintf(file, "%s", "1.0.0");
        return "1.0.0";
    } else {
        char buf[1024];
        fgets(buf, 1024, file);
        return buf;
    }
}

/**
 * @brief This function is called whenever a new connection is opened.
 * @param client Client connection.
 */
void WebSocketServer::onOpen(ws_cli_conn_t *client) {
    Json::Value json;
    json["type"] = "auth";
    json["version"] = version;
    ws_sendframe_txt(client, json.toStyledString().c_str());
}


/**
 * @brief This function is called whenever a connection is closed.
 * @param client Client connection.
 */
void WebSocketServer::onClose(ws_cli_conn_t *client) {
    //从客户端列表删除
    clients.erase(client);
}

/**
 * @brief Message events goes here.
 * @param client Client connection.
 * @param msg    Message content.
 * @param size   Message size.
 * @param type   Message type.
 */
void WebSocketServer::onMessage(ws_cli_conn_t *client,
                                const unsigned char *msg, uint64_t size, int type) {




    try {
        Json::Value json;
        Json::Reader reader;
        if (!reader.parse((const char *) msg, json)) {
            printf("json parse error\n");
            return;
        }
        printf("recived: %s\n", json.toStyledString().c_str());

        std::string message_id = json["id"].asString();
        std::string message_type = json["type"].asString();
        std::string message_token = json["token"].asString();

        printf("message_type: %s\n", message_type.c_str());


        Json::Value ret;
        if (message_type == "auth") {


            if (json["data"].asString() != token) {
                printf("token error %s\n",json["data"].asString().c_str());
                printf("token error %s\n",token.c_str());
                publishToken();
                ws_close_client(client);
                return;
            }
            clients[client] = true;
            ret["type"] = "auth/success";
            ret["id"] = message_id;
            ret["data"] = "OK";
            ws_sendframe_txt(client, ret.toStyledString().c_str());
            return;
        }

        if (clients.find(client) == clients.end() && message_token != token) {
            printf("client not auth\n");
            ws_close_client(client);
            return;
        }


        ret["data"] = "OK";
        //对于不同的路由进行处理
        Json::Value data = json["data"];

        if(message_type == "hi,server"){
            ret["data"] = "hi,client";
        }else if (message_type == "log/put") {
            DbManager::getInstance().insertLog(data["date"].asString(), data["app"].asString(),
                                               data["hook"].asInt(), data["thread"].asString(),
                                               data["line"].asString(), data["log"].asString(),data["level"].asInt());

        } else if (message_type == "log/get") {
            ret["data"] = DbManager::getInstance().getLog(data["limit"].asInt());
        }else if (message_type == "log/delete/all") {
            DbManager::getInstance().deleteAllLog();
        }



        else if (message_type == "setting/set") {
            std::string app = data["app"].asString();
            std::string key = data["key"].asString();
            std::string value = data["value"].asString();
            DbManager::getInstance().setSetting(app, key, value);
        } else if (message_type == "setting/get") {
            std::string app = data["app"].asString();
            std::string key = data["key"].asString();
            ret["data"] = DbManager::getInstance().getSetting(app, key);
        }



        else if(message_type == "bill/put"){

            int id = data["id"].asInt();
            int _type = data["type"].asInt();
            std::string currency = data["currency"].asString();
            int money = data["money"].asInt();
            int fee = data["fee"].asInt();
            int timeStamp = data["timeStamp"].asInt();
            std::string shopName = data["shopName"].asString();
            std::string cateName = data["cateName"].asString();
            std::string extendData = data["extendData"].asString();
            std::string bookName = data["bookName"].asString();
            std::string bookId = data["bookId"].asString();
            std::string accountNameFrom = data["accountNameFrom"].asString();
            std::string accountNameTo = data["accountNameTo"].asString();
            std::string fromApp = data["fromApp"].asString();
            int groupId = data["groupId"].asInt();
            std::string channel = data["channel"].asString();
            int syncFromApp = data["syncFromApp"].asInt();
            std::string remark = data["remark"].asString();
            int fromType = data["fromType"].asInt();

            ret["data"]=DbManager::getInstance().insertBill(id, _type, currency, money, fee, timeStamp, shopName, cateName, extendData, bookName, bookId, accountNameFrom, accountNameTo, fromApp, groupId, channel, syncFromApp, remark, fromType);
        }else if(message_type == "bill/sync/list"){
            ret["data"]=DbManager::getInstance().getWaitSyncBills();
            //要求账单app每次同步完后都要发送一个消息给服务器，服务器更新状态
        } else if(message_type == "bill/sync/update"){
            int id = data["id"].asInt();
            int status = data["status"].asInt();
            DbManager::getInstance().updateBillSyncStatus(id, status);
        } else if(message_type == "bill/list/group"){
            ret["data"]=DbManager::getInstance().getBillListGroup(data["limit"].asInt());
        } else if(message_type == "bill/list/id"){
            ret["data"]=DbManager::getInstance().getBillByIds(data["ids"].asString());
        } else if(message_type == "bill/list/child"){
            ret["data"]=DbManager::getInstance().getBillByGroupId(data["groupId"].asInt());
        }else if(message_type == "bill/list/parent"){
            ret["data"]=DbManager::getInstance().getBillAllParents();
        }


        else if(message_type == "data/put"){
            int id = data["id"].asInt();
            std::string _data = data["data"].asString();
            int _type = data["type"].asInt();
            std::string source = data["source"].asString();
            int timeStamp = data["timeStamp"].asInt();
            int match = data["match"].asInt();
            int issue = data["issue"].asInt();
            std::string rule = data["rule"].asString();
            DbManager::getInstance().insertAppData(id, _data, _type, source,rule, timeStamp, match, issue);
        } else if(message_type == "data/get"){
            ret["data"]=DbManager::getInstance().getAppData(data["limit"].asInt());
        } else if(message_type == "data/delete/all") {
            DbManager::getInstance().deleteAllAppData();
        } else if (message_type == "data/delete/id") {
            int id = data["id"].asInt();
            DbManager::getInstance().deleteAppData(id);
        }


        else if(message_type == "asset/put"){
            std::string id = data["id"].asString();
            std::string name = data["name"].asString();
            int _type = data["type"].asInt();
            int sort = data["sort"].asInt();
            std::string icon = data["icon"].asString();
            std::string extra = data["extra"].asString();
            DbManager::getInstance().insertAsset(id, name, _type, sort, icon, extra);
        } else if(message_type == "asset/get"){
            ret["data"]=DbManager::getInstance().getAsset(data["limit"].asInt());
        } else if(message_type == "asset/get/name"){
            ret["data"]=DbManager::getInstance().getAssetByName(data["name"].asString());
        } else if(message_type == "asset/remove"){
            std::string name = data["name"].asString();
            DbManager::getInstance().removeAsset(name);
        } else if(message_type == "asset/remove/all"){
            DbManager::getInstance().removeAssetAll();
        }


        else if(message_type == "asset/map/put"){
            int id = data["id"].asInt();
            std::string name = data["name"].asString();
            std::string mapName = data["mapName"].asString();
            int regex = data["regex"].asInt();
            DbManager::getInstance().insertAssetMap(id, name, mapName, regex);
        } else if(message_type == "asset/map/get"){
            ret["data"]=DbManager::getInstance().getAssetMap();
        } else if(message_type == "asset/map/remove"){
            std::string id = data["id"].asString();
            DbManager::getInstance().removeAssetMap(id);
        }


        else if(message_type == "book/put"){
            std::string id = data["id"].asString();
            std::string name = data["name"].asString();
            std::string icon = data["icon"].asString();
            DbManager::getInstance().insertBookName(id, name, icon);
        } else if(message_type == "book/get/one"){
            ret["data"] = DbManager::getInstance().getOneBookName();
        } else if(message_type == "book/get/name"){
            ret["data"] = DbManager::getInstance().getBookName(data["name"].asString());
        } else if(message_type == "book/get/all"){
            ret["data"] = DbManager::getInstance().getBookName();
        } else if(message_type == "book/remove"){
            std::string name = data["name"].asString();
            DbManager::getInstance().removeBookName(name);
        }

        else if(message_type == "book/sync"){
            //TODO 来自
        }
        else if(message_type == "assets/sync"){
            //TODO 来自
        }else if(message_type == "app/bill/add"){
            //TODO 来自
        }

        else if(message_type == "cate/put"){
            std::string id = data["id"].asString();
            std::string name = data["name"].asString();
            std::string icon = data["icon"].asString();
            std::string remoteId = data["remoteId"].asString();
            std::string parent = data["parent"].asString();
            std::string book = data["book"].asString();
            int sort = data["sort"].asInt();
            int _type = data["type"].asInt();
            DbManager::getInstance().insertCate(id, name, icon, remoteId, parent, book, sort, _type);
        } else if(message_type == "cate/get/all"){
            std::string book = data["book"].asString();
            int _type = data["type"].asInt();
            std::string parent = data["parent"].asString();
            ret["data"] = DbManager::getInstance().getAllCate(parent, book, _type);
        } else if(message_type == "cate/get/book"){
            std::string book = data["book"].asString();
            ret["data"] = DbManager::getInstance().getBookAllCate(book);
        } else if(message_type == "cate/get/id"){
            std::string book = data["book"].asString();
            std::string name = data["name"].asString();
            int _type = data["type"].asInt();
            ret["data"] = DbManager::getInstance().getCate(book, name,_type);
        } else if(message_type == "cate/get/remote"){
            std::string book = data["book"].asString();
            std::string remoteId = data["remoteId"].asString();
            ret["data"] = DbManager::getInstance().getCateByRemote(book, remoteId);
        } else if(message_type == "cate/remove"){
            std::string id = data["id"].asString();
            DbManager::getInstance().removeCate(id);
        } else if(message_type == "cate/remove/all"){
            DbManager::getInstance().removeCateAll();
        }

        else if(message_type == "rule/custom/put"){
            int id = data["id"].asInt();
            std::string js = data["js"].asString();
            std::string text = data["text"].asString();
            std::string element = data["element"].asString();
            int use = data["use"].asInt();
            int sort = data["sort"].asInt();
            int _auto = data["auto"].asInt();
            DbManager::getInstance().insertCustomRule(id, js, text, element, use, sort, _auto);
        } else if(message_type == "rule/custom/get"){
            ret["data"] = DbManager::getInstance().loadCustomRules(data["limit"].asInt());
        } else if(message_type == "rule/custom/remove"){
            int id = data["id"].asInt();
            DbManager::getInstance().removeCustomRule(id);
        } else if(message_type == "rule/custom/get/id"){
            int id = data["id"].asInt();
            ret["data"] = DbManager::getInstance().getCustomRule(id);
        }


        else if(message_type == "rule/put"){
            std::string app = data["app"].asString();
            std::string js = data["js"].asString();
            std::string _version = data["version"].asString();
            int _type = data["type"].asInt();
            DbManager::getInstance().insertRule(app, js, _version, _type);
        } else if(message_type == "rule/get"){
            std::string app = data["app"].asString();
            int _type = data["type"].asInt();
            ret["data"] = DbManager::getInstance().getRule(app, _type);
        } else if (message_type == "rule/setting/get") {
            int limit = data["limit"].asInt();
            ret["data"] = DbManager::getInstance().getRule(limit);
        } else if (message_type == "rule/setting/put") {
            int id = data["id"].asInt();
            int autoAccounting = data["autoAccounting"].asInt();
            int enable = data["enable"].asInt();
            DbManager::getInstance().ruleSetting(id, autoAccounting, enable);
        } else if (message_type == "rule/remove") {
            int id = data["id"].asInt();
            DbManager::getInstance().removeRule(id);
        }



        else if(message_type == "analyze"){
            int id = data["id"].asInt();
            std::string _data = data["data"].asString();
            std::string app = data["app"].asString();
            int _type = data["type"].asInt();
            int call = data["call"].asInt();
            int timeStamp = std::time(nullptr);
            if (call == 1) {
                // 先存data
                id = DbManager::getInstance().insertAppData(id, _data, _type, app, "", timeStamp, 0, 0);
            }

            //Json::Value rule = DbManager::getInstance().getRule(app, _type);
            std::string rule = DbManager::getInstance().getSetting("server", "rule_js");
            //先执行分析账单内容
            std::string billJs =
                    "var window = {data:JSON.stringify(" + _data + "), type:" +
                    std::to_string(_type) + ",app:'" + app + "'};" +
                    rule;

            std::string result = runJs(billJs);

            Json::Value _json;
            Json::Reader _reader;
            if (!_reader.parse((const char *) result.c_str(), _json)) {
                printf("json parse error\n");
                ret["data"] = "json parse error";
            }else{
                double money = _json["money"].asDouble();
                _json["money"] = std::to_string(money);
                int bill_type = _json["type"].asInt();
                std::string shopName = replaceSubstring( _json["shopName"].asString(),"'","\"");
                std::string shopItem = replaceSubstring( _json["shopItem"].asString(),"'","\"");
                std::time_t now = std::time(nullptr);
                //time时间戳格式化为：HH:mm
                char buffer[32];
                std::tm *ptm = std::localtime(&now);
                std::strftime(buffer, 32, "%H:%M", ptm);
                std::string timeStr = buffer;

                std::string channel = _json["channel"].asString();

                //自动重新更新，不需要App调用更新
                DbManager::getInstance().insertAppData(id, _data, _type, app, channel, timeStamp, 1, 0);

                //分析分类内容
                std::pair<bool, bool> pair = DbManager::getInstance().checkRule(app, _type,
                                                                                channel);

                if (!pair.first && call == 1) {
                    //不启用这个规则
                    ret["data"] = "rule not enable";
                }else{
                    Json::Value JsList = DbManager::getInstance().loadCustomRules(500);
                    std::string customJs;

                    for (const Json::Value& item : JsList) {
                        // 检查 "js" 键是否存在并且是字符串类型
                        if (item.isMember("js") && item["js"].isString()) {
                            customJs += item["js"].asString() + "\n"; // 将每个 js 字段的值合并，添加换行符
                        }
                    }
                    std::string official_cate_js = DbManager::getInstance().getSetting("server",
                                                                                       "official_cate_js");
                    std::string categoryJs =
                            "var window = {money:" + std::to_string(money) + ", type:" +
                            std::to_string(bill_type) + ", shopName:'" + shopName +
                            "', shopItem:'" + shopItem + "', time:'" + timeStr + "'};\n" +
                            "function getCategory(money,type,shopName,shopItem,time){ " + customJs +
                            " return null};\n" +
                            "var categoryInfo = getCategory(window.money,window.type,window.shopName,window.shopItem,window.time);" +
                            "if(categoryInfo !== null) { print(JSON.stringify(categoryInfo));  } else { " +
                            official_cate_js + " }";

                    std::string categoryResult = runJs(categoryJs);
                    Json::Value categoryJson;
                    if (!_reader.parse(categoryResult, categoryJson)) {
                        printf("json parse error\n");
                        ret["data"] = "json parse error";
                    } else {
                        // 兼容措施
                        std::string bookName = categoryJson["book"].asString();
                        std::string cateName = categoryJson["category"].asString();

                        _json["bookName"] = bookName;
                        _json["cateName"] = cateName;
                        _json["timeStamp"] = _json["time"]; // 参数纠正
                        _json["fromApp"] = app; // 补全缺失参数

                        //兼容措施
                        if (bill_type == 1) {
                            _json["accountNameTo"] = _json["accountNameFrom"];
                            _json["accountNameFrom"] = "";
                        }


                        //拉起自动记账app
                        if (call == 1) {
                            try {
                                std::string cmd =
                                        R"(am start -a "net.ankio.auto.ACTION_SHOW_FLOATING_WINDOW" -d "autoaccounting://bill?data=)" +
                                        base64::to_base64(_json.toStyledString()) +
                                        R"(" --ez "android.intent.extra.NO_ANIMATION" true -f 0x10000000)";
                                //写日志
                                log("执行命令" + cmd, LOG_LEVEL_INFO);
                                system(cmd.c_str());
                            } catch (const std::exception &e) {
                                log("拉起自动记账失败：" + std::string(e.what()), LOG_LEVEL_ERROR);
                            }

                        }

                        ret["data"] = _json;
                    }
                }



            }




        }


        else {
            ret["data"] = "error";
        }


        ret["type"] = message_type;
        ret["id"] = message_id;
        ws_sendframe_txt(client, ret.toStyledString().c_str());
    } catch (std::exception &e) {
        log("error: " + std::string(e.what()), LOG_LEVEL_ERROR);
    }


  //ws_sendframe_txt(client, "hello");


}

std::string WebSocketServer::generateRandomString(int count) {
    std::string str = "0123456789abcdefghijklmnopqrstuvwxyz";
    std::random_device rd;
    std::mt19937 generator(rd());
    std::shuffle(str.begin(), str.end(), generator);
    return str.substr(0, count);
}

void WebSocketServer::initToken() {
    FILE *file = fopen("token.txt", "r");
    if (file == nullptr) {
        file = fopen("token.txt", "w");
        token = generateRandomString(32);
        fprintf(file, "%s", token.c_str());
    } else {
        char buf[1024];
        fgets(buf, 1024, file);
        token = buf;
    }

    trim(token);

    fclose(file);

    publishToken();
}



void WebSocketServer::publishToken() {
    //检查是否存在apps.txt，如果有就逐行读取
    FILE *appsFile = fopen("apps.txt", "r");
    if (appsFile != nullptr) {
        char buf[1024];
        while (fgets(buf, 1024, appsFile) != nullptr) {
            //读取包名拼接目录，将token写入目录
            std::string app = std::string(buf);
            trim(app);
            std::string appPath = std::string("/sdcard/Android/data/") + app;
            if (std::filesystem::exists(appPath)) {
                std::string path = appPath + "/token.txt";
                FILE *appFile = fopen(path.c_str(), "w");
                printf("write token to %s\n", path.c_str());
                fprintf(appFile, "%s", token.c_str());
                fclose(appFile);
                chmod(path.c_str(), 0777);
            }
        }
        fclose(appsFile);
    }
}

std::map<ws_cli_conn_t *, bool> WebSocketServer::clients{};
std::string WebSocketServer::token;

// 将 std::thread::id 转换为字符串
std::string threadIdToString(const std::thread::id& id) {
    std::ostringstream oss;
    oss << id;
    return oss.str();
}


void WebSocketServer::print(qjs::rest<std::string> args) {
    log(args[0], LOG_LEVEL_DEBUG);
    std::lock_guard<std::mutex> lock(resultMapMutex);
    resultMap[std::this_thread::get_id()] = args[0];
}

void WebSocketServer::log(const std::string &msg,int level ){
    std::time_t now = std::time(nullptr);
    std::tm *ptm = std::localtime(&now);
    char buffer[32];
    // 格式化日期和时间：YYYY-MM-DD HH:MM:SS
    std::strftime(buffer, 32, "%Y-%m-%d %H:%M:%S", ptm);
    //获取当前时间
    std::string  date = {buffer};
    //获取堆栈信息
    DbManager::getInstance().insertLog(date, "server", 0, "main", "server", msg,level);
    printf("[ %s ] %s\n", buffer, msg.c_str());
}


std::string WebSocketServer::runJs(const std::string &js) {
    log("执行JS脚本",LOG_LEVEL_INFO);
    // log("脚本内容: " + js,LOG_LEVEL_DEBUG);
    qjs::Runtime runtime;
    qjs::Context context(runtime);
    std::thread::id id = std::this_thread::get_id();
    try {
        auto &module = context.addModule("MyModule");
        module.function<&WebSocketServer::print>("print");
        context.eval(R"xxx(
             import { print } from 'MyModule';
            globalThis.print = print;
        )xxx", "<import>", JS_EVAL_TYPE_MODULE);

        context.eval(js);
        std::lock_guard<std::mutex> lock(resultMapMutex);
        std::string data = resultMap[id];
        resultMap.erase(id);
        return data;
    }
    catch (qjs::exception &e) {
        auto exc = context.getException();
        log("JS Error: " + (std::string) exc,LOG_LEVEL_WARN);
        if ((bool) exc["stack"])
            log("JS Error: " + (std::string) exc["stack"],LOG_LEVEL_WARN);
    }
    return "";
}
std::map<std::thread::id, std::string> WebSocketServer::resultMap;
std::mutex WebSocketServer::resultMapMutex;