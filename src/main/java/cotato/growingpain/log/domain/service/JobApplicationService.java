package cotato.growingpain.log.domain.service;

import cotato.growingpain.member.repository.MemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class JobApplicationService {

    private final MemberRepository memberRepository;


}
