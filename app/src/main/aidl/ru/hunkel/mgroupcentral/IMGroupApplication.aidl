package ru.hunkel.mgroupcentral;

interface IMGroupApplication {
    oneway void register(String appPackage);
    oneway void registerWithSettings(String appPackage,String appSettings);
}
