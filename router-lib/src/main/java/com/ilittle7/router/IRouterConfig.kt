package com.ilittle7.router

/**
 * 路由器全局配置接口，通过 ServiceLoader 加载
 */
interface IRouterConfig {
    val baseUri: String?
}
