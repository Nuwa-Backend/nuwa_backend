package org.project.nuwabackend.service.message;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.project.nuwabackend.domain.mongo.DirectMessage;
import org.project.nuwabackend.domain.workspace.WorkSpaceMember;
import org.project.nuwabackend.dto.message.request.DirectMessageRequestDto;
import org.project.nuwabackend.dto.message.response.DirectMessageResponseDto;
import org.project.nuwabackend.global.exception.NotFoundException;
import org.project.nuwabackend.repository.jpa.WorkSpaceMemberRepository;
import org.project.nuwabackend.repository.mongo.DirectMessageRepository;
import org.project.nuwabackend.service.notification.NotificationService;
import org.project.nuwabackend.service.auth.JwtUtil;
import org.project.nuwabackend.service.channel.DirectChannelRedisService;
import org.project.nuwabackend.type.NotificationType;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

import static org.project.nuwabackend.global.type.ErrorMessage.WORK_SPACE_MEMBER_NOT_FOUND;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DirectMessageService {

    private final WorkSpaceMemberRepository workSpaceMemberRepository;
    private final DirectChannelRedisService directChannelRedisService;
    private final DirectMessageRepository directMessageRepository;
    private final JwtUtil jwtUtil;

    private final NotificationService notificationService;
    private static final String PREFIX_URL = "http://localhost:3000/";

    // 메세지 저장
    @Transactional
    public void saveDirectMessage(DirectMessageResponseDto directMessageResponseDto) {
        log.info("메세지 저장");
        Long workSpaceId = directMessageResponseDto.workSpaceId();
        String directChannelRoomId = directMessageResponseDto.roomId();
        Long senderId = directMessageResponseDto.senderId();
        String senderName = directMessageResponseDto.senderName();
        String directContent = directMessageResponseDto.content();
        Long readCount = directMessageResponseDto.readCount();
        LocalDateTime createdAt = directMessageResponseDto.createdAt();

        DirectMessage directMessage = DirectMessage.createDirectMessage(
                workSpaceId,
                directChannelRoomId,
                senderId,
                senderName,
                directContent,
                readCount,
                createdAt);

        directMessageRepository.save(directMessage);
    }

    // 저장된 메세지 가져오기 (Slice)
    public Slice<DirectMessageResponseDto> directMessageSliceOrderByCreatedDate(String directChannelRoomId, Pageable pageable) {
        log.info("저장된 메세지 가져오기");
        return directMessageRepository.findDirectMessageByRoomIdOrderByCreatedAtDesc(directChannelRoomId, pageable)
                .map(directMessage -> DirectMessageResponseDto.builder()
                        .workSpaceId(directMessage.getWorkSpaceId())
                        .roomId(directMessage.getRoomId())
                        .senderId(directMessage.getSenderId())
                        .senderName(directMessage.getSenderName())
                        .content(directMessage.getContent())
                        .readCount(directMessage.getReadCount())
                        .createdAt(directMessage.getCreatedAt())
                        .build());
    }

    // 메세지 보내기
    @Transactional
    public DirectMessageResponseDto sendMessage(String accessToken, DirectMessageRequestDto directMessageRequestDto) {

        log.info("메세지 보내기");
        String email = jwtUtil.getEmail(accessToken);
        Long workSpaceId = directMessageRequestDto.workSpaceId();
        String directChannelRoomId = directMessageRequestDto.roomId();
        String directChannelContent = directMessageRequestDto.content();
        Long receiverId = directMessageRequestDto.receiverId();

        // 메세지 보낸 사람
        WorkSpaceMember sender = workSpaceMemberRepository.findByMemberEmailAndWorkSpaceId(email, workSpaceId)
                .orElseThrow(() -> new NotFoundException(WORK_SPACE_MEMBER_NOT_FOUND));

        Long senderId = sender.getId();
        String senderName = sender.getName();

        // 메세지 받는 사람 (알림용)
        WorkSpaceMember receiver = workSpaceMemberRepository.findById(receiverId)
                .orElseThrow(() -> new NotFoundException(WORK_SPACE_MEMBER_NOT_FOUND));

        boolean isAllConnected = directChannelRedisService.isAllConnected(directChannelRoomId);

        Long readCount = isAllConnected ? 0L : 1L;

        // 채팅을 읽지 않았을 때 알림을 전송
        // 읽었을 땐 알림을 보내지 않습니다.
        if (!readCount.equals(0L)) {
            log.info("알림 전송");
            notificationService.send(
                    directChannelContent,
                    createDirectUrl(directChannelRoomId),
                    NotificationType.DIRECT,
                    receiver);
        }

        return DirectMessageResponseDto.builder()
                .workSpaceId(workSpaceId)
                .roomId(directChannelRoomId)
                .senderId(senderId)
                .senderName(senderName)
                .content(directChannelContent)
                .readCount(readCount)
                .createdAt(LocalDateTime.now())
                .build();
    }

    // TODO: 프론트 주소 확인해서 url 생성 해야함
    private String createDirectUrl(String directChannelRoomId) {
        return PREFIX_URL + directChannelRoomId;
    }
}



