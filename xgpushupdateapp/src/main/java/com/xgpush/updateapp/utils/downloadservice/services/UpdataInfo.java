package com.xgpush.updateapp.utils.downloadservice.services;

/**
 * 存放更新文件解析后的内容的对象
 */
public class UpdataInfo {
    private String version;
    private String url;
    private String description;
    private String url_server;

    public String getUrl_server() {
        return url_server;
    }

    public void setUrl_server(String url_server) {
        this.url_server = url_server;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

}
