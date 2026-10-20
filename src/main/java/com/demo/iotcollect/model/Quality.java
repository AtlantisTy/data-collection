package com.demo.iotcollect.model;

/**
 * 单次采样的数据质量。
 * GOOD = 正常读取到工程值；BAD = 读取失败/超时/从站不可达。
 */
public enum Quality {
    GOOD,
    BAD
}