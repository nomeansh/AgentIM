package org.dromara.common.core.service.impl;

import org.dromara.common.core.service.PermissionService;
import org.springframework.stereotype.Service;

import java.util.Set;

/**
 * 权限服务
 *
 * @author Lion Li
 */
@Service
public class PermissionServiceImpl implements PermissionService {

    @Override
    public Set<String> getRolePermission(Long userId) {
        return Set.of();
    }

    @Override
    public Set<String> getMenuPermission(Long userId) {
        return Set.of();
    }

}
