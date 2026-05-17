package org.dromara.common.translation.core.impl;

import org.dromara.common.core.utils.StringUtils;
import org.dromara.common.translation.annotation.TranslationType;
import org.dromara.common.translation.constant.TransConstant;
import org.dromara.common.translation.core.TranslationInterface;
import lombok.AllArgsConstructor;

/**
 * 用户名翻译实现
 *
 * @author Lion Li
 */
@AllArgsConstructor
@TranslationType(type = TransConstant.USER_ID_TO_NAME)
public class UserNameTranslationImpl implements TranslationInterface<String> {

    @Override
    public String translation(Object key, String other) {
        return key == null ? null : StringUtils.blankToDefault(key.toString(), StringUtils.EMPTY);
    }
}
