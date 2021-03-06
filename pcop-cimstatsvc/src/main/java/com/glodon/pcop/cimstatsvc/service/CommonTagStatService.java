package com.glodon.pcop.cimstatsvc.service;

import com.glodon.pcop.cim.engine.dataServiceCache.CimCacheManager;
import com.glodon.pcop.cim.engine.dataServiceEngine.dataMart.RelationDirection;
import com.glodon.pcop.cim.engine.dataServiceEngine.dataServiceBureau.CimDataSpace;
import com.glodon.pcop.cim.engine.dataServiceEngine.util.exception.CimDataEngineDataMartException;
import com.glodon.pcop.cim.engine.dataServiceEngine.util.factory.CimDataEngineComponentFactory;
import com.glodon.pcop.cim.engine.dataServiceFeature.BusinessLogicConstant;
import com.glodon.pcop.cim.engine.dataServiceFeature.vo.CommonTagVO;
import com.glodon.pcop.cim.engine.dataServiceModelAPI.model.*;
import com.glodon.pcop.cim.engine.dataServiceModelAPI.modelImpl.CommonTagsDSImpl;
import com.glodon.pcop.cim.engine.dataServiceModelAPI.transferVO.UniversalDimensionAttachInfo;
import com.glodon.pcop.cim.engine.dataServiceModelAPI.util.exception.DataServiceModelRuntimeException;
import com.glodon.pcop.cim.engine.dataServiceModelAPI.util.factory.ModelAPIComponentFactory;
import com.glodon.pcop.cimstatsvc.dao.DbExecute;
import com.glodon.pcop.cimstatsvc.model.CommonTagData;
import com.glodon.pcop.cimstatsvc.model.StatParameter;
import org.ehcache.Cache;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.glodon.pcop.cimstatsvc.dao.DbExecute.getCommonTag;

@Service
public class CommonTagStatService {
    private static  String relationType = "BUSINESS_BUILDIN_RELATIONTYPE_COMMONTAG";
    private static String  cacheName = "CIM_STAT_SVC_CACHE_COMMONTAG";
    @Autowired
    private CimStatService cimStatService;


    public CommonTagData commonTagStat(CimDataSpace cimDataSpace,List<StatParameter> statParameterList, String tenantId, String tagName) {
        CommonTag commonTag = getCommonTag(cimDataSpace,tagName,tenantId);
        return commonTagStat(cimDataSpace, statParameterList, tenantId,  commonTag);
    }

    // 缓存所有tag之间的关系

    //找到所有要查询的类

    //生成批量参数

    //对查询结果进行处理


    public   List<String> getObjectTypeId(CommonTag commonTag){
        List<InfoObjectDef> objectDefList = commonTag.getAttachedInfoObjectDefs(relationType, RelationDirection.TWO_WAY);
        List<String> objectTypeIds = new ArrayList<>();
        for (int i = 0; i < objectDefList.size(); i++) {
            objectTypeIds.add(objectDefList.get(i).getObjectTypeName());
        }
        List<CommonTag> list = commonTag.getChildTags();
        for (int i = 0; i < list.size(); i++) {
            objectTypeIds.addAll(getObjectTypeId(list.get(i)));
        }
        return objectTypeIds;
    }

    public CommonTagData commonTagStatBatch(List<StatParameter> statParameterList, String tenantId, CommonTag commonTag ){
        List<String> objectTypeIds = getObjectTypeId(commonTag);
        System.out.println(objectTypeIds.toString());
        List<Map<String, Object>> result =  statBatch(objectTypeIds, statParameterList, tenantId);
        CommonTagData  commonTagData = new CommonTagData();
        commonTagData.setStat(result);
        return  commonTagData;
    }

    public List<Map<String, Object>>  statBatch(List<String> objectTypeIds, List<StatParameter> statParameterList, String tenantId) {
        List<StatParameter> list = new ArrayList<>();
        for (int i = 0; i < objectTypeIds.size(); i++) {
            for (int j = 0; j < statParameterList.size(); j++) {
                StatParameter statParameter = statParameterFilter(statParameterList.get(j),objectTypeIds.get(i));
                list.add(statParameter);
            }
        }
        StatParameter[] statParameters = new StatParameter[list.size()];
        list.toArray(statParameters);

        List<Map<String, Object>> result = cimStatService.stat(statParameters, tenantId);
        return  result;
    }


    public static  ArrayList<String> getTypeNameByCim(CommonTag commonTag){
        Cache<String,ArrayList> cache = CimCacheManager.getOrCreateCache(cacheName, String.class,ArrayList.class);
        if (cache != null) {
            if (cache.containsKey(commonTag.getTagRID())) {
                return  cache.get(commonTag.getTagRID());
            }
        }
        List<InfoObjectDef> objectDefList = commonTag.getAttachedInfoObjectDefs(relationType, RelationDirection.TWO_WAY);
        ArrayList<String> objectTypeIds = new ArrayList<>();
        for (int i = 0; i < objectDefList.size(); i++) {
            objectTypeIds.add(objectDefList.get(i).getObjectTypeName());
        }
        cache.put(commonTag.getTagRID(),objectTypeIds);
        return  objectTypeIds;
    }

    public CommonTagData commonTagStat(CimDataSpace cimDataSpace,List<StatParameter> statParameterList, String tenantId, CommonTag commonTag ) {
        StatParameter[] statParameters = new StatParameter[statParameterList.size()];
        statParameterList.toArray(statParameters);
        List<String> values = new ArrayList<>();
        List<String> keys = statParameters[0].getGroupAttributes();
        for (int i = 0; i < statParameters.length; i++) {
            StatParameter s = statParameters[i];
            values.add(s.getStatPro());
        }

        CommonTagVO commonTagVO = new CommonTagVO();
        commonTagVO.setTagDesc(commonTag.getTagDesc());
        commonTagVO.setTagName(commonTag.getTagName());
        List<CommonTag> list = commonTag.getChildTags();

        //处理子节点的数据
        List<CommonTagData> commonTagDatalist = new ArrayList<>();
        for (int i = 0; i < list.size(); i++) {
            commonTagDatalist.add(commonTagStat(cimDataSpace,statParameterList, tenantId, list.get(i)));
        }

        List<Map<String, Object>> result = new ArrayList<>();
        for (int i = 0; i < commonTagDatalist.size(); i++) {
            result.addAll(commonTagDatalist.get(i).getStat());
        }
        //处理关联对象的数据
//        List<InfoObjectDef> objectDefList = commonTag.getAttachedInfoObjectDefs(relationType, RelationDirection.TWO_WAY);
//        List<String> objectTypeIds = new ArrayList<>();
//        for (int i = 0; i < objectDefList.size(); i++) {
//            objectTypeIds.add(objectDefList.get(i).getObjectTypeName());
//        }
        List<String> objectTypeIds = getTypeNameByCim(commonTag);
        if(objectTypeIds.size() > 0) {
            List<Map<String, Object>> ress = stat(cimDataSpace, objectTypeIds, statParameterList, tenantId);
            result.addAll(ress);
            System.out.println(commonTag.getTagName()+ "\t"+ ress.toString());
        }

        result = stat(result, keys, values);

        CommonTagData commonTagData = new CommonTagData();

        commonTagData.setStat(result);
        commonTagData.setCommonTag(commonTagVO);
        commonTagData.setChildren(commonTagDatalist);
        return commonTagData;
    }


    public StatParameter  statParameterFilter(StatParameter statParameter,String objecTypeId){
        StatParameter statParameter1 = new StatParameter();
        statParameter1.setProperty(statParameter.getProperty());
        statParameter1.setStatType(statParameter.getStatType());
        statParameter1.setGroupAttributes(statParameter.getGroupAttributes());
        statParameter1.setConditions(statParameter.getConditions());
        statParameter1.setCim_object_type(objecTypeId);
//        statParameter1.setStat_item(objecTypeId);
        return statParameter1;
    }

    //将所有对象类型合并
    public List<Map<String, Object>> stat(CimDataSpace  cimDataSpace, List<String> objectTypeIds, List<StatParameter> statParameterList, String tenantId) {

        List<StatParameter> list = new ArrayList<>();

        for (int i = 0; i < objectTypeIds.size(); i++) {
            for (int j = 0; j < statParameterList.size(); j++) {
                StatParameter statParameter = statParameterFilter(statParameterList.get(j),objectTypeIds.get(i));
                list.add(statParameter);
            }
        }
        StatParameter[] statParameters = new StatParameter[list.size()];
        list.toArray(statParameters);

        List<Map<String, Object>> result = cimStatService.stat(cimDataSpace,statParameters, tenantId);

        List<String> values = new ArrayList<>();
        List<String> keys = statParameterList.get(0).getGroupAttributes();

        for (int i = 0; i < statParameterList.size(); i++) {
            StatParameter s = statParameterList.get(i);
            values.add(s.getStatPro());
        }
        return stat(result, keys,values);
    }


    //将所有对象类型合并同类项
    public List<Map<String, Object>> stat(List<Map<String, Object>> ss2, List<String> keys, List<String> values) {
        List<Map<String, Object>> result = new ArrayList<>();
        for(int i =0;i<ss2.size();i++){
            Map<String, Object> SS = new HashMap<>();
            for(String key : ss2.get(i).keySet()){
                SS.put(key,ss2.get(i).get(key));
            }
            for (int k = 0; k < values.size(); k++) {
                if(SS.get(values.get(k)) == null){
                    SS.put(values.get(k),0);
                }
            }

            result.add(SS);
        }


        Map<String, Map<String, Object>> map = new HashMap<>();
        for (int i = 0; i < result.size(); i++) {
            Map<String, Object> SS = result.get(i);
            String key = "key:";
            if (keys != null) {
                for (int j = 0; j < keys.size(); j++) {
                    if (SS.get(keys.get(j)) != null) {
                        key = key + SS.get(keys.get(j)).toString();
                    }
                }
            }
            if (map.get(key) == null) {
                map.put(key, SS);
            } else {
                Map<String, Object> old = map.get(key);
                for (int k = 0; k < values.size(); k++) {
                    if( SS.get(values.get(k)) != null){
                        if(SS.get(values.get(k)).toString().equals("0")){
                            continue;
                        }
                    }
                    if (old.get(values.get(k)) == null && SS.get(values.get(k)) != null) {
                        old.put(values.get(k), SS.get(values.get(k)));
                    } else if (old.get(values.get(k)) != null && SS.get(values.get(k)) == null) {
                        continue;
                    } else if (old.get(values.get(k)) != null && SS.get(values.get(k)) != null) {
                        if (old.get(values.get(k)) instanceof Integer) {
                            old.put(values.get(k), (int) old.get(values.get(k)) + (int) SS.get(values.get(k)));
                        } else if (old.get(values.get(k)) instanceof Double ) {
                            old.put(values.get(k), (double) old.get(values.get(k)) +  (double)SS.get(values.get(k)));
                        } else if (old.get(values.get(k)) instanceof Float) {
                            old.put(values.get(k), (float) old.get(values.get(k)) + (float) SS.get(values.get(k)));
                        }
                    }
                    if (old.get(values.get(k)) == null && SS.get(values.get(k)) == null) {
                        old.put(values.get(k), 0);
                    }
                }
                map.put(key, old);
            }
        }
        List<Map<String, Object>> result1 = new ArrayList<>();
        for (String key : map.keySet()) {
            result1.add(map.get(key));
        }
        return result1;
    }


    /////////////////////////////////////////tag//////////////////////////////////////

    public static  void tag(){
        String cimSpaceName = "pcopcim";
        String  tenantId = "1";
        String  relatinType = "BUSINESS_BUILDIN_RELATIONTYPE_COMMONTAG";
        CIMModelCore targetCIMModelCore = ModelAPIComponentFactory.getCIMModelCore(cimSpaceName, tenantId);
        RelationTypeDefs relationTypeDefs= targetCIMModelCore.getRelationTypeDefs();
        relationTypeDefs.addRelationTypeDef(relatinType ,"标签相关");
        Map<String,Object> relationDataMap = new HashMap<>();
        UniversalDimensionAttachInfo universalDimensionAttachInfo=new UniversalDimensionAttachInfo(relatinType, RelationDirection.TO,relationDataMap);

        CimDataSpace ids = CimDataEngineComponentFactory.connectInfoDiscoverSpace(cimSpaceName);
        try {
            ids.addDimensionType(BusinessLogicConstant.CIM_TAG_DIMENSION_TYPE_NAME);
        } catch (CimDataEngineDataMartException e) {
            e.printStackTrace();
        }
        try {
            ids.addRelationType(BusinessLogicConstant.IS_PARENT_TAG_RELATION_TYPE_NAME);
        } catch (CimDataEngineDataMartException e) {
            e.printStackTrace();
        }




        CommonTag father = createCommonTag(null,"UPNetwork_Network","管网资产");
        CommonTag js = createCommonTag(father,"UPNetwork_JS_Network","给水管网");
        CommonTag rq = createCommonTag(father,"UPNetwork_RQ_Network","燃气管网");
        CommonTag ps = createCommonTag(father,"UPNetwork_PS_Network","排水管网");
        CommonTag dl = createCommonTag(father,"UPNetwork_DL_Network","电力管网");
        linkTag(js,universalDimensionAttachInfo,"BIMITEM_1696584213431808");
        linkTag(js,universalDimensionAttachInfo,"BIMITEM_1696583764149728");
        linkTag(js,universalDimensionAttachInfo,"BIMITEM_1696584696530432");
        linkTag(js,universalDimensionAttachInfo,"BIMITEM_1696585139111392");
        linkTag(js,universalDimensionAttachInfo,"BIMITEM_1696585278539232");

        linkTag(rq,universalDimensionAttachInfo,"BIMITEM_1696918993479168");
        linkTag(rq,universalDimensionAttachInfo,"BIMITEM_1696926523598336");
        linkTag(rq,universalDimensionAttachInfo,"BIMITEM_1696938155984352");

        linkTag(ps,universalDimensionAttachInfo,"BIMITEM_1696916165838336");
        linkTag(ps,universalDimensionAttachInfo,"BIMITEM_1696917132207616");
        linkTag(ps,universalDimensionAttachInfo,"BIMITEM_1696918062573024");

        linkTag(dl,universalDimensionAttachInfo,"BIMITEM_1696913376323072");
        linkTag(dl,universalDimensionAttachInfo,"BIMITEM_1696914318525952");
        linkTag(dl,universalDimensionAttachInfo,"BIMITEM_1697414795281920");
//        CommonTag father2 = createCommonTag(fatherFather,"publicFacilities","市容环境");
//        CommonTag father1 = createCommonTag(fatherFather,"","道路交通");
//        CommonTag father2 = createCommonTag(fatherFather,"publicFacilities","市容环境");
//        CommonTag father3 = createCommonTag(fatherFather,"publicFacilities","园林绿化");
//        CommonTag father4 = createCommonTag(fatherFather,"room","房屋土地");
//        CommonTag father5 = createCommonTag(fatherFather,"otherFacilities","其他设施");
//        CommonTag father = createCommonTag(null,"cityComponentMonitoring","城市部件监测");
//        CommonTag ledDev = createCommonTag(father,"led","LED显示屏");
//        CommonTag publicBroadcastingDev = createCommonTag(father,"public","公共广播");
//        CommonTag wifiDev = createCommonTag(father,"wifi","无线WIFI");
//        CommonTag envmonitordevice = createCommonTag(father,"env","环境监测");
//        CommonTag guideDisplay  = createCommonTag(father,"guideDisplay","停车诱导屏");
//        CommonTag parkking = createCommonTag(father,"parking","停车场");

////
//        linkTag(LampPostDev,universalDimensionAttachInfo,"LampPostDev");
////        linkTag(ledDev,universalDimensionAttachInfo,"ledDev");
////        linkTag(publicBroadcastingDev,universalDimensionAttachInfo,"publicBroadcastingDev");
////        linkTag(wifiDev,universalDimensionAttachInfo,"wifiDev");
////        linkTag(envmonitordevice,universalDimensionAttachInfo,"envmonitordevice");
////        linkTag(guideDisplay,universalDimensionAttachInfo,"guideDisplay");
////        linkTag(parkking,universalDimensionAttachInfo,"parking");
//        linkTag(cameraDev,universalDimensionAttachInfo,"cameraDev");
//        linkTag(cameraDev,universalDimensionAttachInfo,"aloneCameraDev");
//        linkTag(wellDev,universalDimensionAttachInfo,"pipePointWell");
    }
    public static void  linkTag( CommonTag father,UniversalDimensionAttachInfo ss,String objectTypeId) {
        String cimSpaceName = "pcopcim";
        String  tenantId = "1";
        InfoObjectDef  video1 = getObjType(cimSpaceName,  objectTypeId,  tenantId);
        try {
            RelationInstance relationInstance1 = video1.attachCommonTag(ss,father.getTagRID());
        } catch (DataServiceModelRuntimeException e) {
            e.printStackTrace();
        }
    }

    public static InfoObjectDef getObjType(String cimSpaceName, String objName, String tenantId) {
        CIMModelCore cimModelCore = ModelAPIComponentFactory.getCIMModelCore(cimSpaceName, tenantId);
        InfoObjectDefs objectDefs = cimModelCore.getInfoObjectDefs();
        return objectDefs.getInfoObjectDef(objName);
    }


    public static CommonTag createCommonTag( CommonTag father,   String name,String desc){
        CommonTagVO commonTagVO1=new CommonTagVO();
        commonTagVO1.setTagName(name);
        commonTagVO1.setTagDesc(desc);
        String cimSpaceName = "pcopcim";
        String  tenantId = "1";
        CommonTag addResultTag = null;
//        CommonTags commonTags = new CommonTagsDSImpl(cimSpaceName, tenantId);
        CIMModelCore targetCIMModelCore =
                ModelAPIComponentFactory.getCIMModelCore(cimSpaceName,tenantId);
        CommonTags commonTags = targetCIMModelCore.getCommonTags();
        if(father == null) {
            try {
                addResultTag = commonTags.addTag(commonTagVO1);
            } catch (DataServiceModelRuntimeException e) {
                e.printStackTrace();
            }
        }else{
            try {
                addResultTag = commonTags.addChildTag(commonTagVO1,father.getTagRID());
            } catch (DataServiceModelRuntimeException e) {
                e.printStackTrace();
            }
        }
        return  addResultTag;
    }

}
