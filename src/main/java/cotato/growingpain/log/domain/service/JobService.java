package cotato.growingpain.log.domain.service;

import cotato.growingpain.log.domain.dto.JobPostRequestDTO;
import cotato.growingpain.log.domain.entity.JobApplication;
import cotato.growingpain.log.domain.entity.JobPost;
import cotato.growingpain.log.domain.repository.ApplicationDetailRepository;
import cotato.growingpain.log.domain.repository.JobApplicationRepository;
import cotato.growingpain.log.domain.repository.JobPostRepository;
import cotato.growingpain.member.domain.entity.Member;
import cotato.growingpain.member.repository.MemberRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class JobService {

    private final JobPostRepository jobPostRepository;

    private final MemberRepository memberRepository;

    private final JobApplicationRepository jobApplicationRepository;

    private final ApplicationDetailRepository applicationDetailRepository;

    public JobPost createJobPost(JobPostRequestDTO jobPostRequestDTO) {
        Long memberId = jobPostRequestDTO.memberId();
        log.debug("Trying to fetch member with ID: {}", memberId);

        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new RuntimeException("Member not found with ID: " + memberId));

        log.info("Found member with ID: {}", member.getId());
        log.info(jobPostRequestDTO.jobApplications().toString());

        JobPost jobPost = jobPostRequestDTO.toEntity(member);

        log.info("Creating job post with job part: {}", jobPost.getJobPart());
        log.debug("JobPost details: {}", jobPost);

        JobPost savedJobPost = jobPostRepository.save(jobPost);

        jobPost.getJobApplications().forEach(jobApplication -> {
            jobApplication.setJobPost(savedJobPost);
            JobApplication savedJobApplication = jobApplicationRepository.save(jobApplication);

            jobApplication.getApplicationDetails().forEach(applicationDetail -> {
                applicationDetail.setJobApplication(savedJobApplication);
                applicationDetailRepository.save(applicationDetail);
            });
        });

        return savedJobPost;
    }

}
