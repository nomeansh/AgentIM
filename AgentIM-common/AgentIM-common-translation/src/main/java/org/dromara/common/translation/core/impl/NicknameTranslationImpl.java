package org.dromara.common.translation.core.impl;

import lombok.AllArgsConstructor;
import org.dromara.common.core.utils.StringUtils;
import org.dromara.common.translation.annotation.TranslationType;
import org.dromara.common.translation.constant.TransConstant;
import org.dromara.common.translation.core.TranslationInterface;

/**
 * 用户昵称翻译实现
 *
 * @author may
 */
@AllArgsConstructor
@TranslationType(type = TransConstant.USER_ID_TO_NICKNAME)
public class NicknameTranslationImpl implements TranslationInterface<String> {

    @Override
    public String translation(Object key, String other) {
        return key == null ? null : StringUtils.blankToDefault(key.toString(), StringUtils.EMPTY);
    }
}
