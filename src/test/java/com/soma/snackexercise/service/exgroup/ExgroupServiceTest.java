package com.soma.snackexercise.service.exgroup;

import com.soma.snackexercise.domain.exgroup.Exgroup;
import com.soma.snackexercise.domain.joinlist.JoinList;
import com.soma.snackexercise.domain.member.Member;
import com.soma.snackexercise.dto.exgroup.request.ExgroupCreateRequest;
import com.soma.snackexercise.dto.exgroup.request.ExgroupUpdateRequest;
import com.soma.snackexercise.dto.exgroup.response.ExgroupCreateResponse;
import com.soma.snackexercise.dto.exgroup.response.ExgroupResponse;
import com.soma.snackexercise.dto.member.JoinListMemberDto;
import com.soma.snackexercise.dto.member.response.GetOneGroupMemberResponse;
import com.soma.snackexercise.exception.*;
import com.soma.snackexercise.repository.exgroup.ExgroupRepository;
import com.soma.snackexercise.repository.joinlist.JoinListRepository;
import com.soma.snackexercise.repository.member.MemberRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static com.soma.snackexercise.domain.joinlist.JoinType.HOST;
import static com.soma.snackexercise.domain.joinlist.JoinType.MEMBER;
import static com.soma.snackexercise.factory.dto.ExgroupCreateFactory.createExgroupCreateRequest;
import static com.soma.snackexercise.factory.dto.ExgroupUpdateFactory.createExgroupUpdateRequest;
import static com.soma.snackexercise.factory.entity.ExgroupFactory.createExgroup;
import static com.soma.snackexercise.factory.entity.JoinListFactory.createJoinListForHost;
import static com.soma.snackexercise.factory.entity.JoinListFactory.createJoinListForMember;
import static com.soma.snackexercise.factory.entity.MemberFactory.createMember;
import static com.soma.snackexercise.util.constant.Status.ACTIVE;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
@DisplayName("ExgroupService 비즈니스 로직 테스트")
class ExgroupServiceTest {
    @InjectMocks
    private ExgroupService exgroupService;
    @Mock
    private ExgroupRepository exgroupRepository;
    @Mock
    private MemberRepository memberRepository;
    @Mock
    private JoinListRepository joinListRepository;

    private String email = "test@naver.com";

    @Test
    @DisplayName("운동 그룹 생성 메소드 성공 테스트")
    void createTest() {
        //given
        ExgroupCreateRequest request = createExgroupCreateRequest();
        given(memberRepository.findByEmailAndStatus(anyString(), any())).willReturn(Optional.of(createMember()));
        given(exgroupRepository.existsByCode(anyString())).willReturn(Boolean.FALSE);

        // when
        ExgroupCreateResponse response = exgroupService.create(request, email);

        // then
        assertThat(request.getName()).isEqualTo(response.getName());
    }

    @Test
    @DisplayName("운동 그룹 생성 메소드에서 그룹 생성할 회원 조회 예외 클래스 발생 테스트")
    void createExceptionByMemberNotFoundTest() {
        // given
        given(memberRepository.findByEmailAndStatus(anyString(), any())).willReturn(Optional.empty());

        // when, then
        assertThatThrownBy(() -> exgroupService.create(createExgroupCreateRequest(), email)).isInstanceOf(MemberNotFoundException.class);
    }

    @Test
    @DisplayName("운동 그룹 생성 메소드에서 그룹 코드 중복 검사 테스트")
    void createDuplicateGroupCodeTest() {
        // given
        given(memberRepository.findByEmailAndStatus(anyString(), any())).willReturn(Optional.of(createMember()));
        given(exgroupRepository.existsByCode(anyString())).willReturn(Boolean.TRUE, Boolean.TRUE, Boolean.FALSE);

        // when
        exgroupService.create(createExgroupCreateRequest(), email);

        // then
        verify(exgroupRepository, times(3)).existsByCode(anyString());
    }

    @Test
    @DisplayName("운동 그룹 조회 메소드 성공 테스트")
    void readTest() {
        // given
        Exgroup exgroup = createExgroup();
        given(exgroupRepository.findByIdAndStatus(anyLong(), any())).willReturn(Optional.ofNullable(exgroup));

        // when
        ExgroupResponse response = exgroupService.read(1L);

        // then
        assertThat(response.getName()).isEqualTo(exgroup.getName());
    }

    @Test
    @DisplayName("운동 그룹 조회 메소드에서 찾을 수 없을 때 예외 클래스 발생 테스트")
    void readExceptionByExgroupNotFoundTest() {
        // given
        given(exgroupRepository.findByIdAndStatus(anyLong(), any())).willReturn(Optional.ofNullable(null));

        // when, then
        assertThatThrownBy(() -> exgroupService.read(1L)).isInstanceOf(ExgroupNotFoundException.class);
    }

    @Test
    @DisplayName("그룹에 속한 모든 멤버들을 조회하는 메소드 테스트")
    void getAllExgroupMembersTest() {
        // given
        Member member = createMember();
        Member host = createMember();
        Exgroup exgroup = createExgroup();

        given(memberRepository.findAllGroupMembers(anyLong())).willReturn(
                Arrays.asList(
                        new JoinListMemberDto(member, createJoinListForMember(member, exgroup)),
                        new JoinListMemberDto(host, createJoinListForHost(host, exgroup))
                ));

        // when
        List<GetOneGroupMemberResponse> response = exgroupService.getAllExgroupMembers(anyLong());

        // then
        assertThat(response.size()).isEqualTo(2);
        assertThat(response.stream().map(GetOneGroupMemberResponse::getJoinType)).containsExactlyInAnyOrder(HOST, MEMBER);
        verify(memberRepository, times(1)).findAllGroupMembers(anyLong());
    }

    @Test
    @DisplayName("그룹 수정 메소드 성공 테스트")
    void updateTest() {
        // given
        ExgroupUpdateRequest request = createExgroupUpdateRequest();
        given(exgroupRepository.findByIdAndStatus(anyLong(), any())).willReturn(Optional.ofNullable(createExgroup()));
        given(memberRepository.findByEmailAndStatus(anyString(), any())).willReturn(Optional.ofNullable(createMember()));
        given(joinListRepository.existsByExgroupAndMemberAndJoinTypeAndStatus(any(), any(), any(), any())).willReturn(true);
        given(joinListRepository.countByExgroupAndOutCountLessThanOneAndStatusEqualsActive(any())).willReturn(request.getMaxMemberNum());

        // when
        ExgroupResponse response = exgroupService.update(1L, email, request);

        // then
        assertNotNull(response);
        assertThat(response.getName()).isEqualTo(request.getName());
        verify(exgroupRepository, times(1)).findByIdAndStatus(anyLong(), any());
        verify(memberRepository, times(1)).findByEmailAndStatus(anyString(), any());
        verify(joinListRepository, times(1)).existsByExgroupAndMemberAndJoinTypeAndStatus(any(), any(), any(), any());
        verify(joinListRepository, times(1)).countByExgroupAndOutCountLessThanOneAndStatusEqualsActive(any());
    }

    @Test
    @DisplayName("그룹 수정 메소드에서 방장 권한이 아닌 멤버일 때 예외 클래스 발생 테스트")
    void updateExceptionByNotExgroupHostExceptionTest() {
        // given
        ExgroupUpdateRequest request = createExgroupUpdateRequest();
        given(exgroupRepository.findByIdAndStatus(anyLong(), any())).willReturn(Optional.ofNullable(createExgroup()));
        given(memberRepository.findByEmailAndStatus(anyString(), any())).willReturn(Optional.ofNullable(createMember()));
        given(joinListRepository.existsByExgroupAndMemberAndJoinTypeAndStatus(any(), any(), any(), any())).willReturn(false);

        // when, then
        assertThatThrownBy(() -> exgroupService.update(1L, email, request)).isInstanceOf(NotExgroupHostException.class);
    }

    @Test
    @DisplayName("그룹 수정 메소드에서 현재 그룹 인원수가 수정할 최대 인원 수보다 많을 때 발생하는 예외 클래스 발생 테스트")
    void updateExceptionByMaxMemberNumLessThanCurrentExceptionTest() {
        // given
        ExgroupUpdateRequest request = createExgroupUpdateRequest();
        given(exgroupRepository.findByIdAndStatus(anyLong(), any())).willReturn(Optional.ofNullable(createExgroup()));
        given(memberRepository.findByEmailAndStatus(anyString(), any())).willReturn(Optional.ofNullable(createMember()));
        given(joinListRepository.existsByExgroupAndMemberAndJoinTypeAndStatus(any(), any(), any(), any())).willReturn(true);
        given(joinListRepository.countByExgroupAndOutCountLessThanOneAndStatusEqualsActive(any())).willReturn(request.getMaxMemberNum() + 1);

        // when, then
        assertThatThrownBy(() -> exgroupService.update(1L, email, request)).isInstanceOf(MaxMemberNumLessThanCurrentException.class);
    }

    @Test
    @DisplayName("방장이 회원 강퇴 메소드 성공 테스트")
    void deleteMemberByHostTest() {
        // given
        Member member = createMember();
        Exgroup exgroup = createExgroup();
        JoinList joinList = createJoinListForMember(member, exgroup);

        given(exgroupRepository.findByIdAndStatus(anyLong(), any())).willReturn(Optional.ofNullable(exgroup));
        given(memberRepository.findByEmailAndStatus(anyString(), any())).willReturn(Optional.ofNullable(createMember()));
        given(memberRepository.findByIdAndStatus(anyLong(), any())).willReturn(Optional.ofNullable(member));
        given(joinListRepository.findByExgroupAndMemberAndStatus(any(), any(), any())).willReturn(Optional.ofNullable(joinList));
        given(joinListRepository.existsByExgroupAndMemberAndJoinTypeAndStatus(any(), any(), any(), any())).willReturn(true);

        // when
        exgroupService.deleteMemberByHost(1L, 1L, email);

        // then
        verify(exgroupRepository, times(1)).findByIdAndStatus(anyLong(), any());
        verify(memberRepository, times(1)).findByEmailAndStatus(anyString(), any());
        verify(memberRepository, times(1)).findByIdAndStatus(anyLong(), any());
        verify(joinListRepository, times(1)).findByExgroupAndMemberAndStatus(any(), any(), any());
        verify(joinListRepository, times(2)).existsByExgroupAndMemberAndJoinTypeAndStatus(any(), any(), any(), any());
    }

    @Test
    @DisplayName("방장이 회원 강퇴 메소드에서 현재 사용자가 방장 권한이 아닐 때 예외클래스 발생 테스트")
    void deleteMemberByHostTestExceptionByNotExgroupHostExceptionTest() {
        // given
        Member member = createMember();
        Exgroup exgroup = createExgroup();
        JoinList joinList = createJoinListForMember(member, exgroup);

        given(exgroupRepository.findByIdAndStatus(anyLong(), any())).willReturn(Optional.ofNullable(exgroup));
        given(memberRepository.findByEmailAndStatus(anyString(), any())).willReturn(Optional.ofNullable(createMember()));
        given(memberRepository.findByIdAndStatus(anyLong(), any())).willReturn(Optional.ofNullable(member));
        given(joinListRepository.findByExgroupAndMemberAndStatus(any(), any(), any())).willReturn(Optional.ofNullable(joinList));
        given(joinListRepository.existsByExgroupAndMemberAndJoinTypeAndStatus(any(), any(), any(), any())).willReturn(false);

        // when, then
        assertThatThrownBy(() -> exgroupService.deleteMemberByHost(1L, 1L, email)).isInstanceOf(NotExgroupHostException.class);
    }

    @Test
    @DisplayName("방장이 회원 강퇴 메소드에서 타겟 사용자가 멤버 권한이 아닐 때 예외클래스 발생 테스트")
    void deleteMemberByHostTestExceptionByNotExgroupMemberExceptionTest() {
        // given
        Member member = createMember();
        Exgroup exgroup = createExgroup();
        JoinList joinList = createJoinListForMember(member, exgroup);

        given(exgroupRepository.findByIdAndStatus(anyLong(), any())).willReturn(Optional.ofNullable(exgroup));
        given(memberRepository.findByEmailAndStatus(anyString(), any())).willReturn(Optional.ofNullable(createMember()));
        given(memberRepository.findByIdAndStatus(anyLong(), any())).willReturn(Optional.ofNullable(member));
        given(joinListRepository.findByExgroupAndMemberAndStatus(any(), any(), any())).willReturn(Optional.ofNullable(joinList));
        // todo : org.mockito.exceptions.misusing.PotentialStubbingProblem
        given(joinListRepository.existsByExgroupAndMemberAndJoinTypeAndStatus(any(Exgroup.class), any(Member.class), eq(HOST), eq(ACTIVE))).willReturn(true);
        given(joinListRepository.existsByExgroupAndMemberAndJoinTypeAndStatus(any(Exgroup.class), any(Member.class), eq(MEMBER), eq(ACTIVE))).willReturn(false);

        // when, then
        assertThatThrownBy(() -> exgroupService.deleteMemberByHost(1L, 1L, email)).isInstanceOf(NotExgroupMemberException.class);
    }

    @Test
    @DisplayName("회원이 그룹을 탈퇴하는 메소드 성공 테스트")
    void leaveGroupByMemberTest() {
        // given
        Member member = createMember();
        Exgroup exgroup = createExgroup();
        JoinList joinList = createJoinListForMember(member, exgroup);

        given(memberRepository.findByEmailAndStatus(anyString(), any())).willReturn(Optional.ofNullable(member));
        given(exgroupRepository.findByIdAndStatus(anyLong(), any())).willReturn(Optional.ofNullable(exgroup));
        given(joinListRepository.findByExgroupAndMemberAndStatus(any(), any(), any())).willReturn(Optional.ofNullable(joinList));
        given(joinListRepository.existsByExgroupAndStatus(any(), any())).willReturn(true);

        // when
        exgroupService.leaveGroupByMember(1L, email);

        // then
        verify(memberRepository, times(1)).findByEmailAndStatus(anyString(), any());
        verify(exgroupRepository, times(1)).findByIdAndStatus(anyLong(), any());
        verify(joinListRepository, times(1)).findByExgroupAndMemberAndStatus(any(), any(), any());
        verify(joinListRepository, times(1)).existsByExgroupAndStatus(any(), any());
        verify(joinListRepository, times(0)).findFirstByExgroupAndStatusOrderByCreatedAtAsc(any(), any());
    }

    @Test
    @DisplayName("회원이 그룹을 탈퇴하는 메소드에서 방장이 탈퇴했을 때 동작 검증 테스트")
    void leaveGroupByMemberByHost() {
        // given
        Member member = createMember();
        Exgroup exgroup = createExgroup();
        JoinList joinList = createJoinListForHost(member, exgroup);

        given(memberRepository.findByEmailAndStatus(anyString(), any())).willReturn(Optional.ofNullable(member));
        given(exgroupRepository.findByIdAndStatus(anyLong(), any())).willReturn(Optional.ofNullable(exgroup));
        given(joinListRepository.findByExgroupAndMemberAndStatus(any(), any(), any())).willReturn(Optional.ofNullable(joinList));
        given(joinListRepository.existsByExgroupAndStatus(any(), any())).willReturn(true);
        given(joinListRepository.findFirstByExgroupAndStatusOrderByCreatedAtAsc(any(), any())).willReturn(Optional.ofNullable(joinList));

        // when
        exgroupService.leaveGroupByMember(1L, email);

        // then
        verify(joinListRepository, times(1)).findFirstByExgroupAndStatusOrderByCreatedAtAsc(any(), any());
    }

    @Test
    @DisplayName("회원이 그룹을 탈퇴하는 메소드에서 방장이 탈퇴했을 때 방장을 찾을 수 없을 때 예외 클래스 동작 테스트")
    void leaveGroupByMemberByHostExceptionByJoinListNotFoundException() {
        // given
        Member member = createMember();
        Exgroup exgroup = createExgroup();
        JoinList joinList = createJoinListForHost(member, exgroup);

        given(memberRepository.findByEmailAndStatus(anyString(), any())).willReturn(Optional.ofNullable(member));
        given(exgroupRepository.findByIdAndStatus(anyLong(), any())).willReturn(Optional.ofNullable(exgroup));
        given(joinListRepository.findByExgroupAndMemberAndStatus(any(), any(), any())).willReturn(Optional.ofNullable(joinList));
        given(joinListRepository.existsByExgroupAndStatus(any(), any())).willReturn(true);
        given(joinListRepository.findFirstByExgroupAndStatusOrderByCreatedAtAsc(any(), any())).willReturn(Optional.empty());

        // when, then
        assertThatThrownBy(() -> exgroupService.leaveGroupByMember(1L, email)).isInstanceOf(JoinListNotFoundException.class);
    }

    @Test
    @DisplayName("방장이 그룹 시작하는 메소드 성공 테스트")
    void startGroupTest() {
        // given
        Member member = createMember();
        Exgroup exgroup = createExgroup();

        given(memberRepository.findByEmailAndStatus(anyString(), any())).willReturn(Optional.ofNullable(member));
        given(exgroupRepository.findByIdAndStatus(anyLong(), any())).willReturn(Optional.ofNullable(exgroup));
        given(joinListRepository.existsByExgroupAndMemberAndJoinTypeAndStatus(any(), any(), any(), any())).willReturn(true);

        // when
        ExgroupResponse response = exgroupService.startGroup(1L, email);

        // then
        verify(memberRepository, times(1)).findByEmailAndStatus(anyString(), any());
        verify(exgroupRepository, times(1)).findByIdAndStatus(anyLong(), any());
        verify(joinListRepository, times(1)).existsByExgroupAndMemberAndJoinTypeAndStatus(any(), any(), any(), any());
        assertThat(response.getName()).isEqualTo(exgroup.getName());
    }

    @Test
    @DisplayName("방장이 그룹 시작하는 메소드에서 현재 사용자가 방장 권한이 아닐 때 예외 클래스 발생 테스트")
    void startGroupByNotExgroupHostExceptionTest() {
        Member member = createMember();
        Exgroup exgroup = createExgroup();

        given(memberRepository.findByEmailAndStatus(anyString(), any())).willReturn(Optional.ofNullable(member));
        given(exgroupRepository.findByIdAndStatus(anyLong(), any())).willReturn(Optional.ofNullable(exgroup));
        given(joinListRepository.existsByExgroupAndMemberAndJoinTypeAndStatus(any(), any(), any(), any())).willReturn(false);

        // when, then
        assertThatThrownBy(() -> exgroupService.startGroup(1L, email)).isInstanceOf(NotExgroupHostException.class);
    }
}