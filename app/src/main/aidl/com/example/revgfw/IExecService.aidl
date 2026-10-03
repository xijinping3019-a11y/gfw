package com.example.revgfw;

interface IExecService {
    void destroy() = 16777114;      // Shizuku 保留方法，别改
    String exec(String cmd) = 1;    // 我们自己加：执行 shell 命令
    int getUid() = 2;               // 用来验证身份
}
