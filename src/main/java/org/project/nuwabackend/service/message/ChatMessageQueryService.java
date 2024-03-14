package org.project.nuwabackend.service.message;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.project.nuwabackend.domain.mongo.ChatMessage;
import org.project.nuwabackend.domain.mongo.DirectMessage;
import org.project.nuwabackend.domain.workspace.WorkSpaceMember;
import org.project.nuwabackend.dto.message.request.MessageDeleteRequestDto;
import org.project.nuwabackend.dto.message.request.MessageUpdateRequestDto;
import org.project.nuwabackend.dto.message.response.MessageDeleteResponseDto;
import org.project.nuwabackend.dto.message.response.MessageUpdateResponseDto;
import org.project.nuwabackend.global.exception.NotFoundException;
import org.project.nuwabackend.repository.jpa.WorkSpaceMemberRepository;
import org.project.nuwabackend.service.auth.JwtUtil;
import org.project.nuwabackend.service.s3.FileService;
import org.project.nuwabackend.type.MessageType;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Service;

import java.util.List;

import static org.project.nuwabackend.global.type.ErrorMessage.WORK_SPACE_MEMBER_NOT_FOUND;
import static org.project.nuwabackend.type.MessageType.DELETE;
import static org.project.nuwabackend.type.MessageType.FILE;
import static org.project.nuwabackend.type.MessageType.IMAGE;
import static org.project.nuwabackend.type.MessageType.UPDATE;

@Slf4j
@Service
@RequiredArgsConstructor
public class ChatMessageQueryService {

    private final WorkSpaceMemberRepository workSpaceMemberRepository;
    private final MongoTemplate mongoTemplate;
    private final FileService fileService;
    private final JwtUtil jwtUtil;

    // 워크스페이스 ID로 관련 채팅 채널 메세지 전부 삭제
    // TODO: integrated test code
    public void deleteChatMessageWorkSpaceId(Long workSpaceId) {
        Query query = new Query(Criteria.where("workspace_id").is(workSpaceId));

        mongoTemplate.remove(query, ChatMessage.class);
    }

    // 파일 URL과 MessageType이 File인거를 찾아서 삭제
    // TODO: integrated test code
    public void deleteChatMessageByFile(Long workSpaceId, String fileUrl) {
        Query query = new Query(Criteria.where("workspace_id").is(workSpaceId)
                .and("chat_content").is(fileUrl)
                .and("message_type").is(FILE));

        mongoTemplate.remove(query, ChatMessage.class);
    }

    // WorkSpace ID와 Room ID에 해당되는 채팅 전부 삭제
    // TODO: integrated test code
    public void deleteWorkSpaceIdAndRoomId(Long workSpaceId, String roomId) {
        Query query = new Query(Criteria.where("workspace_id").is(workSpaceId)
                .and("chat_room_id").is(roomId));

        mongoTemplate.remove(query, ChatMessage.class);
    }

    // 메세지 수정
    // TODO: test code
    public MessageUpdateResponseDto updateChatMessage(String accessToken, MessageUpdateRequestDto messageUpdateRequestDto) {
        String email = jwtUtil.getEmail(accessToken);
        String id = messageUpdateRequestDto.id();
        String roomId = messageUpdateRequestDto.roomId();
        Long workSpaceId = messageUpdateRequestDto.workSpaceId();
        String content = messageUpdateRequestDto.content();
        MessageType messageType = messageUpdateRequestDto.messageType();
        boolean isEdited = messageType.equals(UPDATE);

        WorkSpaceMember sender = workSpaceMemberRepository.findByMemberEmailAndWorkSpaceId(email, workSpaceId)
                .orElseThrow(() -> new NotFoundException(WORK_SPACE_MEMBER_NOT_FOUND));

        Long senderId = sender.getId();

        Query query = new Query(Criteria.where("workspace_id").is(workSpaceId)
                .and("id").is(id)
                .and("chat_room_id").is(roomId)
                .and("chat_sender_id").is(senderId));

        Update update = new Update().set("chat_content", content).set("is_edited", isEdited);
        mongoTemplate.updateMulti(query, update, ChatMessage.class);

        return MessageUpdateResponseDto.builder()
                .id(id)
                .roomId(roomId)
                .workSpaceId(workSpaceId)
                .content(content)
                .messageType(messageType)
                .isEdited(isEdited)
                .build();
    }

    // 메세지 삭제
    // TODO: test code
    public MessageDeleteResponseDto deleteChatMessage(String accessToken, MessageDeleteRequestDto messageDeleteRequestDto) {
        String email = jwtUtil.getEmail(accessToken);
        String id = messageDeleteRequestDto.id();
        String roomId = messageDeleteRequestDto.roomId();
        Long workSpaceId = messageDeleteRequestDto.workSpaceId();
        MessageType messageType = messageDeleteRequestDto.messageType();
        boolean isDeleted = messageType.equals(DELETE);

        WorkSpaceMember sender = workSpaceMemberRepository.findByMemberEmailAndWorkSpaceId(email, workSpaceId)
                .orElseThrow(() -> new NotFoundException(WORK_SPACE_MEMBER_NOT_FOUND));

        Long senderId = sender.getId();

        String content = "삭제된 메세지입니다.";

        Query query = new Query(Criteria.where("workspace_id").is(workSpaceId)
                .and("id").is(id)
                .and("chat_room_id").is(roomId)
                .and("chat_sender_id").is(senderId));

        ChatMessage chatMessage = mongoTemplate.findOne(query, ChatMessage.class);

        if (chatMessage != null && (chatMessage.getMessageType().equals(IMAGE) || chatMessage.getMessageType().equals(FILE))) {
            List<String> rawString = chatMessage.getRawString();

            List<Long> idList = fileService.fileIdList(rawString);

            idList.forEach(fileService::deleteFile);
        }

        Update update = new Update().set("chat_content", content).set("is_deleted", isDeleted).set("raw_string", content);
        mongoTemplate.updateMulti(query, update, ChatMessage.class);

        return MessageDeleteResponseDto.builder()
                .id(id)
                .roomId(roomId)
                .workSpaceId(workSpaceId)
                .content(content)
                .isDeleted(isDeleted)
                .messageType(messageType)
                .build();
    }
}
