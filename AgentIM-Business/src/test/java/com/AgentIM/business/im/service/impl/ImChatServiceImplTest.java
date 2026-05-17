package com.AgentIM.business.im.service.impl;

import com.AgentIM.business.im.constant.ImEventType;
import com.AgentIM.business.im.domain.bo.ImChatMemberBo;
import com.AgentIM.business.im.domain.entity.ImChat;
import com.AgentIM.business.im.domain.entity.ImChatMember;
import com.AgentIM.business.im.domain.entity.ImUser;
import com.AgentIM.business.im.domain.vo.ImChatMemberVo;
import com.AgentIM.business.im.domain.vo.ImChatVo;
import com.AgentIM.business.im.enums.ImChatType;
import com.AgentIM.business.im.enums.ImMemberRole;
import com.AgentIM.business.im.mapper.ImChatMapper;
import com.AgentIM.business.im.mapper.ImChatMemberMapper;
import com.AgentIM.business.im.mapper.ImUserMapper;
import com.AgentIM.business.im.permission.ImPermissionService;
import com.AgentIM.business.im.support.ImAuditService;
import com.AgentIM.business.im.support.ImEventPublisher;
import org.dromara.common.core.exception.ServiceException;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.util.Collection;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Tag("dev")
class ImChatServiceImplTest {

    private final ImChatServiceImpl service = new ImChatServiceImpl(
        stub(ImChatMapper.class),
        stub(ImChatMemberMapper.class),
        stub(ImUserMapper.class),
        new ImPermissionService(null, null, null),
        null,
        null
    );

    @Test
    void normalizeRoleRejectsOwnerFromMemberManagement() {
        assertThrows(ServiceException.class, () -> normalizeRole(ImChatType.GROUP.getCode(), ImMemberRole.OWNER.getCode()));
        assertThrows(ServiceException.class, () -> normalizeRole(ImChatType.CHANNEL.getCode(), ImMemberRole.OWNER.getCode()));
    }

    @Test
    void normalizeRoleRejectsRolesThatDoNotBelongToTheChatType() {
        assertThrows(ServiceException.class, () -> normalizeRole(ImChatType.GROUP.getCode(), ImMemberRole.SUBSCRIBER.getCode()));
        assertThrows(ServiceException.class, () -> normalizeRole(ImChatType.CHANNEL.getCode(), ImMemberRole.MEMBER.getCode()));
    }

    @Test
    void normalizeRoleDefaultsByChatType() {
        assertEquals(ImMemberRole.MEMBER.getCode(), normalizeRole(ImChatType.GROUP.getCode(), null));
        assertEquals(ImMemberRole.SUBSCRIBER.getCode(), normalizeRole(ImChatType.CHANNEL.getCode(), null));
    }

    @Test
    void deletePrivateChatPublishesDeletedEventToCurrentUser() {
        ImChat chat = new ImChat();
        chat.setId(20L);
        chat.setType(ImChatType.PRIVATE.getCode());
        ImChatMember member = new ImChatMember();
        member.setId(30L);
        member.setChatId(20L);
        member.setUserId(10L);
        TestChatMemberMapper memberMapper = new TestChatMemberMapper(member);
        CapturingEventPublisher eventPublisher = new CapturingEventPublisher();
        ImChatServiceImpl privateChatService = new ImChatServiceImpl(
            stub(ImChatMapper.class),
            memberMapper.proxy(),
            stub(ImUserMapper.class),
            new TestPermissionService(10L, chat),
            new NoopAuditService(),
            eventPublisher
        );

        privateChatService.deleteChat(20L);

        assertEquals(30L, memberMapper.deletedId);
        assertEquals(ImEventType.CHAT_DELETED, eventPublisher.eventType);
        assertEquals(20L, eventPublisher.chatId);
        assertEquals(10L, eventPublisher.actorId);
        assertEquals(List.of(10L), eventPublisher.userIds);
        assertTrue(eventPublisher.payload instanceof Map<?, ?>);
        assertEquals(20L, ((Map<?, ?>) eventPublisher.payload).get("id"));
    }

    @Test
    void addMemberPublishesChatSnapshotForUpdatedEvent() {
        ImChat chat = chat(20L, ImChatType.GROUP.getCode());
        TestChatMemberMapper memberMapper = new TestChatMemberMapper(null, memberVo(40L, 30L, ImMemberRole.MEMBER.getCode()));
        CapturingEventPublisher eventPublisher = new CapturingEventPublisher();
        ImChatServiceImpl chatService = new ImChatServiceImpl(
            stub(ImChatMapper.class),
            memberMapper.proxy(),
            activeUserMapper(),
            new TestPermissionService(10L, chat, Map.of(10L, ImMemberRole.OWNER.getCode())),
            new NoopAuditService(),
            eventPublisher
        );

        chatService.addMember(20L, memberBo(30L, ImMemberRole.MEMBER.getCode()));

        assertEquals(ImEventType.CHAT_UPDATED, eventPublisher.chatEventType);
        assertTrue(eventPublisher.chatPayload instanceof ImChatVo);
        assertEquals(ImChatType.GROUP.getCode(), ((ImChatVo) eventPublisher.chatPayload).getType());
    }

    @Test
    void removeMemberPublishesSnapshotToRemainingMembersAndDeletedEventToRemovedUser() {
        ImChat chat = chat(20L, ImChatType.GROUP.getCode());
        ImChatMember member = new ImChatMember();
        member.setId(40L);
        member.setChatId(20L);
        member.setUserId(30L);
        member.setRole(ImMemberRole.MEMBER.getCode());
        TestChatMemberMapper memberMapper = new TestChatMemberMapper(member, memberVo(50L, 10L, ImMemberRole.OWNER.getCode()));
        CapturingEventPublisher eventPublisher = new CapturingEventPublisher();
        ImChatServiceImpl chatService = new ImChatServiceImpl(
            stub(ImChatMapper.class),
            memberMapper.proxy(),
            activeUserMapper(),
            new TestPermissionService(10L, chat, Map.of(10L, ImMemberRole.OWNER.getCode(), 30L, ImMemberRole.MEMBER.getCode())),
            new NoopAuditService(),
            eventPublisher
        );

        chatService.removeMember(20L, 30L);

        assertEquals(ImEventType.CHAT_UPDATED, eventPublisher.chatEventType);
        assertTrue(eventPublisher.chatPayload instanceof ImChatVo);
        assertEquals(ImEventType.CHAT_DELETED, eventPublisher.eventType);
        assertEquals(List.of(30L), eventPublisher.userIds);
    }

    private String normalizeRole(String chatType, String role) {
        try {
            Method method = ImChatServiceImpl.class.getDeclaredMethod("normalizeRole", String.class, String.class);
            method.setAccessible(true);
            return (String) method.invoke(service, chatType, role);
        } catch (InvocationTargetException e) {
            if (e.getCause() instanceof RuntimeException runtimeException) {
                throw runtimeException;
            }
            throw new IllegalStateException(e.getCause());
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException(e);
        }
    }

    private static ImChat chat(Long id, String type) {
        ImChat chat = new ImChat();
        chat.setId(id);
        chat.setType(type);
        return chat;
    }

    private static ImChatMemberBo memberBo(Long userId, String role) {
        ImChatMemberBo bo = new ImChatMemberBo();
        bo.setUserId(userId);
        bo.setRole(role);
        return bo;
    }

    private static ImChatMemberVo memberVo(Long id, Long userId, String role) {
        ImChatMemberVo vo = new ImChatMemberVo();
        vo.setId(id);
        vo.setChatId(20L);
        vo.setUserId(userId);
        vo.setRole(role);
        return vo;
    }

    private static ImUserMapper activeUserMapper() {
        return (ImUserMapper) Proxy.newProxyInstance(
            ImUserMapper.class.getClassLoader(),
            new Class<?>[]{ImUserMapper.class},
            (proxy, method, args) -> {
                if ("selectById".equals(method.getName())) {
                    ImUser user = new ImUser();
                    user.setId((Long) args[0]);
                    user.setStatus("0");
                    return user;
                }
                return defaultValue(method.getReturnType());
            }
        );
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

    private static final class TestChatMemberMapper {
        private final ImChatMember member;
        private final ImChatMemberVo memberVo;
        private Long deletedId;

        private TestChatMemberMapper(ImChatMember member) {
            this(member, null);
        }

        private TestChatMemberMapper(ImChatMember member, ImChatMemberVo memberVo) {
            this.member = member;
            this.memberVo = memberVo;
        }

        private ImChatMemberMapper proxy() {
            return (ImChatMemberMapper) Proxy.newProxyInstance(
                ImChatMemberMapper.class.getClassLoader(),
                new Class<?>[]{ImChatMemberMapper.class},
                (proxy, method, args) -> {
                    if ("selectOne".equals(method.getName())) {
                        return member;
                    }
                    if ("deleteById".equals(method.getName())) {
                        deletedId = (Long) args[0];
                        return 1;
                    }
                    if ("insert".equals(method.getName())) {
                        ImChatMember inserted = (ImChatMember) args[0];
                        inserted.setId(40L);
                        return 1;
                    }
                    if ("selectMembers".equals(method.getName())) {
                        return memberVo == null ? List.of() : List.of(memberVo);
                    }
                    return defaultValue(method.getReturnType());
                }
            );
        }
    }

    private static final class TestPermissionService extends ImPermissionService {
        private final Long userId;
        private final ImChat chat;
        private final Map<Long, String> roles;

        private TestPermissionService(Long userId, ImChat chat) {
            this(userId, chat, Map.of(userId, ImMemberRole.MEMBER.getCode()));
        }

        private TestPermissionService(Long userId, ImChat chat, Map<Long, String> roles) {
            super(null, null, null);
            this.userId = userId;
            this.chat = chat;
            this.roles = roles;
        }

        @Override
        public Long currentUserId() {
            return userId;
        }

        @Override
        public ImChat requireReadable(Long chatId) {
            return chat;
        }

        @Override
        public ImChat requireChat(Long chatId) {
            return chat;
        }

        @Override
        public void requireManager(Long chatId) {
        }

        @Override
        public String roleOf(Long chatId, Long userId) {
            return roles.get(userId);
        }
    }

    private static final class NoopAuditService extends ImAuditService {
        private NoopAuditService() {
            super(null);
        }

        @Override
        public void record(Long chatId, String resourceType, Long resourceId, String action,
                           Long actorId, String summary, Object payload) {
        }
    }

    private static final class CapturingEventPublisher extends ImEventPublisher {
        private String eventType;
        private Long chatId;
        private Long actorId;
        private List<Long> userIds;
        private Object payload;
        private String chatEventType;
        private Object chatPayload;

        private CapturingEventPublisher() {
            super(null, null, null);
        }

        @Override
        public void publishToUsers(String eventType, Long chatId, Long actorId, Collection<Long> userIds, Object payload) {
            this.eventType = eventType;
            this.chatId = chatId;
            this.actorId = actorId;
            this.userIds = List.copyOf(userIds);
            this.payload = payload;
        }

        @Override
        public void publishToChat(String eventType, Long chatId, Long actorId, Object payload) {
            this.chatEventType = eventType;
            this.chatId = chatId;
            this.actorId = actorId;
            this.chatPayload = payload;
        }
    }
}
