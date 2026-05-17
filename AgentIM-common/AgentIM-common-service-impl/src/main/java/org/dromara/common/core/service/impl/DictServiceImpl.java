package org.dromara.common.core.service.impl;

import org.dromara.common.core.service.DictService;
import org.dromara.common.core.utils.StringUtils;
import org.springframework.stereotype.Service;

import java.util.*;

/**
 * 字典服务服务
 *
 * @author Lion Li
 */
@Service
public class DictServiceImpl implements DictService {

    /**
     * 根据字典类型和字典值获取字典标签
     *
     * @param dictType  字典类型
     * @param dictValue 字典值
     * @param separator 分隔符
     * @return 字典标签
     */
    @Override
    public String getDictLabel(String dictType, String dictValue, String separator) {
        return StringUtils.blankToDefault(dictValue, StringUtils.EMPTY);
    }

    /**
     * 根据字典类型和字典标签获取字典值
     *
     * @param dictType  字典类型
     * @param dictLabel 字典标签
     * @param separator 分隔符
     * @return 字典值
     */
    @Override
    public String getDictValue(String dictType, String dictLabel, String separator) {
        return StringUtils.blankToDefault(dictLabel, StringUtils.EMPTY);
    }

    /**
     * 获取字典下所有的字典值与标签
     *
     * @param dictType 字典类型
     * @return dictValue为key，dictLabel为值组成的Map
     */
    @Override
    public Map<String, String> getAllDictByDictType(String dictType) {
        return new HashMap<>();
    }

}
