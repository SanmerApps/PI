package dev.sanmer.su;

interface IService {
    int getUid() = 0;
    int getPid() = 1;
    String getSELinuxContext() = 2;
}