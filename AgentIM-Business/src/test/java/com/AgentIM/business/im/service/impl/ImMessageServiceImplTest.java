package com.AgentIM.business.im.service.impl;

import com.AgentIM.business.im.domain.bo.ImMessageSendBo;
import com.AgentIM.business.im.domain.entity.ImChat;
import com.AgentIM.business.im.domain.entity.ImMessage;
import com.AgentIM.business.im.enums.ImMessageType;
import com.AgentIM.business.im.mapper.ImChatMapper;
import com.AgentIM.business.im.mapper.ImMessageMapper;
import com.AgentIM.business.im.mapper.ImMessageReactionMapper;
import com.AgentIM.business.im.mapper.ImMessageReplyMapper;
import com.AgentIM.business.im.mapper.ImPollMapper;
import com.AgentIM.business.im.mapper.ImPollOptionMapper;
import com.AgentIM.business.im.mapper.ImResourceMapper;
import com.AgentIM.business.im.mapper.ImUserMessageHideMapper;
import com.AgentIM.business.im.permission.ImPermissionService;
import com.AgentIM.business.im.service.IImReadStateService;
import org.dromara.common.core.exception.ServiceException;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Proxy;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

@Tag("dev")
class ImMessageServiceImplTest {

    private final TestResourceMapper resourceMapper = new TestResourceMapper();
    private final ImPermissionService permissionService = new TestPermissionService(10L);

    private final ImMessageServiceImpl service = new ImMessageServiceImpl(
        stub(ImMessageMapper.class),
        stub(ImMessageReplyMapper.class),
        stub(ImMessageReactionMapper.class),
        stub(ImChatMapper.class),
        resourceMapper.proxy(),
        stub(ImUserMessageHideMapper.class),
        stub(ImPollMapper.class),
        stub(ImPollOptionMapper.class),
        permissionService,
        stub(IImReadStateService.class),
        null,
        null
    );

    @Test
    void validateBindableResourcesRejectsResourcesNotOwnedByCurrentUserOrMessage() {
        List<Long> resourceIds = List.of(1001L, 1002L);
        resourceMapper.bindableCount = 1;

        assertThrows(ServiceException.class, () -> service.validateBindableResourcesForMessage(20L, 30L, resourceIds));
    }

    @Test
    void resolveForwardFromChatIdUsesTheLoadedSourceMessage() {
        ImMessage source = new ImMessage();
        source.setChatId(200L);

        assertEquals(200L, service.resolveForwardFromChatId(source));
    }

    @Test
    void sendMessageRejectsBlankIdempotentKeyBeforeAllocatingSequence() {
        ImMessageSendBo bo = new ImMessageSendBo();
        bo.setMessageType(ImMessageType.TEXT.getCode());
        bo.setContent("hello");
        bo.setIdempotentKey(" ");

        ServiceException exception = assertThrows(ServiceException.class, () -> service.sendMessage(20L, bo));

        assertEquals("幂等键不能为空", exception.getMessage());
    }

    @SuppressWarnings("unchecked")
    private static <T> T stub(Class<T> type) {
        return (T) Proxy.newProxyInstance(type.getClassLoader(), new Class<?>[]{type}, (proxy, method, args) -> defaultValue(method.getReturnType()));
    }

    private static Object defaultValue(Class<?> type) {
        if (!type.isPrimitive()) {
            return null;
        }
        if (boolean.class.equals(type)) {
            return false;
        }
        if (void.class.equals(type)) {
            return null;
        }
        return 0;
    }

    private static final class TestResourceMapper {
        private int bindableCount;

        private ImResourceMapper proxy() {
            return (ImResourceMapper) Proxy.newProxyInstance(
                ImResourceMapper.class.getClassLoader(),
                new Class<?>[]{ImResourceMapper.class},
                (proxy, method, args) -> "countBindableResources".equals(method.getName()) ? bindableCount : defaultValue(method.getReturnType())
            );
        }
    }

    private static final class TestPermissionService extends ImPermissionService {
        private final Long userId;

        private TestPermissionService(Long userId) {
            super(null, null, null);
            this.userId = userId;
        }

        @Override
        public Long currentUserId() {
            return userId;
        }

        @Override
        public ImChat requireWritable(Long chatId) {
            ImChat chat = new ImChat();
            chat.setId(chatId);
            return chat;
        }
    }
}
