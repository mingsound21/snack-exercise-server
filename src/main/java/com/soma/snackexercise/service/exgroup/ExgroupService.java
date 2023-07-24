package com.soma.snackexercise.service.exgroup;

import com.soma.snackexercise.domain.exgroup.CreateExgroupCode;
import com.soma.snackexercise.domain.exgroup.Exgroup;
import com.soma.snackexercise.domain.joinlist.JoinList;
import com.soma.snackexercise.domain.joinlist.JoinType;
import com.soma.snackexercise.domain.member.Member;
import com.soma.snackexercise.dto.exgroup.request.ExgroupUpdateRequest;
import com.soma.snackexercise.dto.exgroup.request.PostCreateExgroupRequest;
import com.soma.snackexercise.dto.exgroup.response.ExgroupResponse;
import com.soma.snackexercise.dto.exgroup.response.PostCreateExgroupResponse;
import com.soma.snackexercise.dto.member.JoinListMemberDto;
import com.soma.snackexercise.dto.member.response.GetOneGroupMemberResponse;
import com.soma.snackexercise.exception.*;
import com.soma.snackexercise.repository.exgroup.ExgroupRepository;
import com.soma.snackexercise.repository.joinlist.JoinListRepository;
import com.soma.snackexercise.repository.member.MemberRepository;
import com.soma.snackexercise.util.constant.Status;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

// TODO : jakarata Transactional과 spring Transactional의 차이는 뭘까
@Slf4j
@RequiredArgsConstructor
@Transactional(readOnly = true)
@Service
public class ExgroupService {
    private final ExgroupRepository exgroupRepository;
    private final MemberRepository memberRepository;
    private final JoinListRepository joinListRepository;


    @Transactional
    public PostCreateExgroupResponse createGroup(PostCreateExgroupRequest groupCreateRequest, String email){

        // 1. 그룹 생성할 회원 조회
        Member member = memberRepository.findByEmailAndStatus(email, Status.ACTIVE).orElseThrow(MemberNotFoundException::new);

        // 2. 그룹 코드 생성
        String groupCode = CreateExgroupCode.createGroupCode();
        Boolean flag = exgroupRepository.existsByCode(groupCode); // 코드 중복 여부

        // 3. 그룹 코드 중복 검사
        while(Boolean.TRUE.equals(flag)){ // 코드 중복 검사 불통과시 코드 재발급
            groupCode = CreateExgroupCode.createGroupCode();
            flag = exgroupRepository.existsByCode(groupCode);
        }

        log.info("generate group code : {}", groupCode);

        // 4. 새로운 그룹 생성
        Exgroup newGroup = Exgroup.builder()
                .name(groupCreateRequest.getName())
                .emozi(groupCreateRequest.getEmozi())
                .color(groupCreateRequest.getColor())
                .description(groupCreateRequest.getDescription())
                .maxMemberNum(groupCreateRequest.getMaxMemberNum())
                .goalRelayNum(groupCreateRequest.getGoalRelayNum())
                .startTime(groupCreateRequest.getStartTime())
                .endTime(groupCreateRequest.getEndTime())
                .penalty(groupCreateRequest.getPenalty())
                .code(groupCode)
                .missionIntervalTime(groupCreateRequest.getMissionIntervalTime())
                .checkIntervalTime(groupCreateRequest.getCheckIntervalTime())
                .checkMaxNum(groupCreateRequest.getCheckMaxNum())
                .build();

        exgroupRepository.save(newGroup);

        // 5. 회원이 그룹 연결
        JoinList joinRequest = JoinList.builder()
                .member(member)
                .exgroup(newGroup)
                .joinType(JoinType.HOST)
                .build();

        joinListRepository.save(joinRequest);
        return new PostCreateExgroupResponse(newGroup.getId(), newGroup.getName());
    }

    public ExgroupResponse findGroup(Long groupId){
        Exgroup exgroup = exgroupRepository.findById(groupId).orElseThrow(ExgroupNotFoundException::new);

        return ExgroupResponse.toDto(exgroup);
    }

    public List<GetOneGroupMemberResponse> getAllExgroupMembers(Long groupId){
        List<JoinListMemberDto> allGroupMembers = memberRepository.findAllGroupMembers(groupId);

        return allGroupMembers.stream().map(data -> new GetOneGroupMemberResponse(data.getMember().getProfileImage(), data.getMember().getNickname(), data.getJoinList().getJoinType(), data.getJoinList().getStatus())).toList();
    }

    @Transactional
    public ExgroupResponse update(Long groupId, String email, ExgroupUpdateRequest request) {
        Exgroup exgroup = exgroupRepository.findByIdAndStatus(groupId, Status.ACTIVE).orElseThrow(ExgroupNotFoundException::new);
        Member member = memberRepository.findByEmailAndStatus(email, Status.ACTIVE).orElseThrow(MemberNotFoundException::new);

        // 1. 사용자가 해당 그룹의 방장인지 확인
        if (!joinListRepository.existsByExgroupAndMemberAndJoinTypeAndStatus(exgroup, member, JoinType.HOST, Status.ACTIVE)) {
            throw new NotExgroupHostException();
        }

        // 2. 그룹 최대 참여 인원 수 >= 현재 인원 수인지 판별
        exgroup.updateMaxMemberNum(joinListRepository.countByExgroupAndOutCountLessThanOneAndStatusEqualsActive(exgroup), request.getMaxMemberNum());
        exgroup.update(request);

        return ExgroupResponse.toDto(exgroup);
    }

     // TODO : 만약 미션 수행 중인 회원이 탈퇴당한다면 -> 다음 사람으로 로직 추가
    @Transactional
    public void deleteMember(Long groupId, Long memberId, String email) {
        Exgroup exgroup = exgroupRepository.findByIdAndStatus(groupId, Status.ACTIVE).orElseThrow(ExgroupNotFoundException::new);
        Member currentMember = memberRepository.findByEmailAndStatus(email, Status.ACTIVE).orElseThrow(MemberNotFoundException::new);
        Member targetMember = memberRepository.findByIdAndStatus(memberId, Status.ACTIVE).orElseThrow(MemberNotFoundException::new);
        JoinList joinList = joinListRepository.findByExgroupAndMemberAndStatus(exgroup, targetMember, Status.ACTIVE).orElseThrow(JoinListNotFoundException::new);

        // 1. 사용자가 해당 그룹의 방장인지 확인
        if (!joinListRepository.existsByExgroupAndMemberAndJoinTypeAndStatus(exgroup, currentMember, JoinType.HOST, Status.ACTIVE)) {
            throw new NotExgroupHostException();
        }

        // 2. 삭제 타겟 회원이 해당 그룹의 멤버인지 확인
        if (!joinListRepository.existsByExgroupAndMemberAndJoinTypeAndStatus(exgroup, targetMember, JoinType.MEMBER, Status.ACTIVE)) {
            throw new NotExgroupMemberException();
        }

        // 3. joinList inActive로 변경
        joinList.inActive();

        // 4. joinList outCount + 1
        joinList.addOneOutCount();
    }
}
