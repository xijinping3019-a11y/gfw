package com.example.revgfw;

interface IExecService {
    void destroy() = 16777114;
    int execCode(String cmd) = 1;
    String execOut(String cmd) = 2;
    int uid() = 3;
}
