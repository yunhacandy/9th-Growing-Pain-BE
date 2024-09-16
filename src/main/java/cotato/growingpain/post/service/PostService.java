package cotato.growingpain.post.service;

import cotato.growingpain.comment.domain.entity.Comment;
import cotato.growingpain.comment.repository.CommentRepository;
import cotato.growingpain.common.exception.AppException;
import cotato.growingpain.common.exception.ErrorCode;
import cotato.growingpain.common.exception.ImageException;
import cotato.growingpain.member.domain.entity.Member;
import cotato.growingpain.member.repository.MemberRepository;
import cotato.growingpain.post.PostCategory;
import cotato.growingpain.post.domain.entity.Post;
import cotato.growingpain.post.dto.request.PostRequest;
import cotato.growingpain.post.repository.PostLikeRepository;
import cotato.growingpain.post.repository.PostRepository;
import cotato.growingpain.replycomment.repository.ReplyCommentRepository;
import cotato.growingpain.s3.S3Uploader;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Slf4j
@Service
@RequiredArgsConstructor
public class PostService {

    private final PostRepository postRepository;
    private final PostLikeRepository postLikeRepository;
    private final MemberRepository memberRepository;
    private final CommentRepository commentRepository;
    private final ReplyCommentRepository replyCommentRepository;
    private final S3Uploader s3Uploader;

    @Transactional
    public void registerPost(PostRequest request, Long memberId) throws ImageException {
        Member member = memberRepository.getReferenceById(memberId);
        PostCategory parentCategory = request.category().getParent();

        String imageUrl = null;
        MultipartFile imageFile = request.postImage();  // postImage를 가져옴
        if (imageFile != null && !imageFile.isEmpty()) {  // null 체크 추가
            imageUrl = s3Uploader.uploadFileToS3(imageFile, "post");
        }

        postRepository.save(
                Post.of(member, request.title(), request.content(), imageUrl, parentCategory,
                        request.category())
        );
    }

    @Transactional
    public List<Post> getPostsByMemberId(Long memberId) {
        return postRepository.findAllByMemberIdAndIsDeletedFalse(memberId);
    }

    @Transactional
    public List<Post> getPostsByCategory(PostCategory category) {
        return postRepository.findAllByCategoryAndIsDeletedFalse(category);
    }

    @Transactional
    public List<Post> getAllPostsByCategory() {
        return postRepository.findAllByIsDeletedFalse();
    }

    @Transactional
    public void deletePost(Long postId, Long memberId) {
        Post post = findByPostIdAndMemberId(postId, memberId);

        if (post.isDeleted()) {
            throw new AppException(ErrorCode.ALREADY_DELETED);
        }

        List<Comment> comments = commentRepository.findAllByPostIdAndIsDeletedFalse(postId);
        for (Comment comment : comments) {
            replyCommentRepository.deleteAllByCommentId(comment.getId());
            commentRepository.delete(comment);
        }

        postLikeRepository.deleteAllByPostId(postId);

        post.deletePost();
        postRepository.save(post);
    }

    @Transactional
    public void updatePost(Long postId, PostRequest request, Long memberId) throws ImageException {
        Post post = findByPostIdAndMemberId(postId, memberId);

        if (post.isDeleted()) {
            throw new AppException(ErrorCode.ALREADY_DELETED);
        }

        String imageUrl = null;
        MultipartFile imageFile = request.postImage();  // postImage를 가져옴
        if (imageFile != null && !imageFile.isEmpty()) {  // null 체크 추가
            imageUrl = s3Uploader.uploadFileToS3(imageFile, "post");
        }

        post.updatePost(request.title(), request.content(), imageUrl, request.category());
        postRepository.save(post);
    }

    private Post findByPostIdAndMemberId(Long postId, Long memberId) {
        return postRepository.findAllByIdAndMemberIdAndIsDeletedFalse(postId, memberId)
                .orElseThrow(() -> new AppException(ErrorCode.POST_NOT_FOUND));
    }
}