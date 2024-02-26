package org.project.nuwabackend.service.s3;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.project.nuwabackend.domain.channel.Channel;
import org.project.nuwabackend.domain.channel.Chat;
import org.project.nuwabackend.domain.member.Member;
import org.project.nuwabackend.domain.multimedia.File;
import org.project.nuwabackend.domain.workspace.WorkSpace;
import org.project.nuwabackend.domain.workspace.WorkSpaceMember;
import org.project.nuwabackend.dto.file.request.FileRequestDto;
import org.project.nuwabackend.dto.file.response.FileUploadResponseDto;
import org.project.nuwabackend.dto.file.response.FileUploadResultDto;
import org.project.nuwabackend.repository.jpa.ChannelRepository;
import org.project.nuwabackend.repository.jpa.FileRepository;
import org.project.nuwabackend.repository.jpa.WorkSpaceMemberRepository;
import org.project.nuwabackend.type.FileType;
import org.project.nuwabackend.type.WorkSpaceMemberType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.text.Normalizer;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;

@DisplayName("[Service] File Service Test")
@ExtendWith(MockitoExtension.class)
class FileServiceTest {

    @Mock
    WorkSpaceMemberRepository workSpaceMemberRepository;
    @Mock
    ChannelRepository channelRepository;
    @Mock
    FileRepository fileRepository;
    @Mock
    S3Service s3Service;

    @InjectMocks
    FileService fileService;

    Member member;
    WorkSpace workSpace;
    WorkSpaceMember workSpaceMember;
    Channel channel;
    MultipartFile multipartFile;
    FileUploadResultDto fileUploadResultDto;
    FileRequestDto fileRequestDto;

    String email = "abcd@gmail.com";


    @BeforeEach
    void setup() throws IOException {
        String workSpaceName = "nuwa";
        String workSpaceImage = "N";
        String workSpaceIntroduce = "개발";

        workSpace = WorkSpace.createWorkSpace(workSpaceName, workSpaceImage, workSpaceIntroduce);

        String password = "abcd1234";
        String nickname = "nickname";
        String phoneNumber = "01000000000";

        member = Member.createMember(email, password, nickname, phoneNumber);

        String workSpaceMemberName = "abcd";
        String workSpaceMemberJob = "backend";
        String workSpaceMemberImage = "B";

        workSpaceMember = WorkSpaceMember.createWorkSpaceMember(
                workSpaceMemberName,
                workSpaceMemberJob,
                workSpaceMemberImage,
                WorkSpaceMemberType.CREATED,
                member, workSpace);

        String channelName = "channelName";
        channel = Chat.createChatChannel(channelName, workSpace, workSpaceMember);

        multipartFile =
                new MockMultipartFile("test", "test.jpg", "image/jpeg", new ByteArrayInputStream("test".getBytes()));

        String uploadName = "test_" + LocalDateTime.now() + ".jpg";
        String url = "https://test-bucket.s3.amazonaws.com/image/direct/" + uploadName;
        Long byteSize = 1024L;
        Map<String, Long> imageUrlMap = new HashMap<>();
        Map<String, Long> fileUrlMap = new HashMap<>();
        imageUrlMap.put(url, byteSize);

        fileUploadResultDto = new FileUploadResultDto(imageUrlMap, fileUrlMap);

        Long workSpaceId = 1L;
        Long channelId = 1L;
        fileRequestDto = new FileRequestDto(workSpaceId, channelId);
    }

    @Test
    @DisplayName("[Service] File Upload Test")
    void uploadTest() {
        //given
        given(workSpaceMemberRepository.findByMemberEmailAndWorkSpaceId(anyString(), any()))
                .willReturn(Optional.of(workSpaceMember));
        given(channelRepository.findById(any()))
                .willReturn(Optional.of(channel));

        given(s3Service.upload(anyString(), any()))
                .willReturn(fileUploadResultDto);

        List<MultipartFile> multipartFileList = new ArrayList<>(List.of(multipartFile));

        Map<String, Long> fileUrlMap = fileUploadResultDto.uploadImageUrlMap();

        List<File> fileList = new ArrayList<>();
        fileUrlMap.forEach((key, value) -> {
            int slashIndex = key.lastIndexOf("/") + 1;
            int underBarIndex = key.lastIndexOf("_");

            String decode = URLDecoder.decode(key.substring(slashIndex, underBarIndex), StandardCharsets.UTF_8).trim();
            String originFileName = Normalizer.normalize(decode, Normalizer.Form.NFC);

            int dotIndex = key.lastIndexOf(".") + 1;

            String fileExtension = key.substring(dotIndex);

            File file =
                    File.createFile(key, originFileName, value, fileExtension,
                            FileType.IMAGE, workSpaceMember, workSpace, channel);
            ReflectionTestUtils.setField(file, "id", 1L);

            fileList.add(file);
        });

        given(fileRepository.saveAll(any()))
                .willReturn(fileList);

        List<FileUploadResponseDto> fileUploadResponseDtoList = fileList.stream().map(file -> FileUploadResponseDto.builder()
                        .fileId(file.getId())
                        .fileType(file.getFileType())
                        .build())
                .toList();
        //when
        List<FileUploadResponseDto> uploadList = fileService.upload(email, multipartFileList, fileRequestDto);

        //then
        assertThat(uploadList.get(0).fileId()).isEqualTo(fileUploadResponseDtoList.get(0).fileId());
        assertThat(uploadList.get(0).fileType()).isEqualTo(fileUploadResponseDtoList.get(0).fileType());
        assertThat(uploadList).containsAll(fileUploadResponseDtoList);

    }
}