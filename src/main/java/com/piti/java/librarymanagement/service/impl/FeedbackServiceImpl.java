package com.piti.java.librarymanagement.service.impl;

import java.util.List;
import java.util.Objects;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

import com.piti.java.librarymanagement.dto.FeedbackRequest;
import com.piti.java.librarymanagement.dto.FeedbackResponse;
import com.piti.java.librarymanagement.dto.PageResponse;
import com.piti.java.librarymanagement.exception.OperationNotPermittedException;
import com.piti.java.librarymanagement.mapper.FeedbackMapper;
import com.piti.java.librarymanagement.model.Book;
import com.piti.java.librarymanagement.model.Feedback;
import com.piti.java.librarymanagement.model.User;
import com.piti.java.librarymanagement.repository.BookRepository;
import com.piti.java.librarymanagement.repository.FeedBackRepository;
import com.piti.java.librarymanagement.service.FeedbackService;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class FeedbackServiceImpl implements FeedbackService{
	private final FeedBackRepository feedBackRepository;
    private final BookRepository bookRepository;
    private final FeedbackMapper feedbackMapper;

	@Override
	public Integer save(FeedbackRequest request, Authentication connectedUser) {
		Book book = bookRepository.findById(request.bookId())
                .orElseThrow(() -> new EntityNotFoundException("No book found with ID:: " + request.bookId()));
        if (book.isArchived() || !book.isShareable()) {
            throw new OperationNotPermittedException("You cannot give a feedback for and archived or not shareable book");
        }
        User user = ((User) connectedUser.getPrincipal());
        if (Objects.equals(book.getOwner().getId(), user.getId())) {
            throw new OperationNotPermittedException("You cannot give feedback to your own book");
        }
        Feedback feedback = feedbackMapper.toFeedback(request);
        return feedBackRepository.save(feedback).getId();
	}

	@Override
	public PageResponse<FeedbackResponse> findAllFeedbacksByBook(Integer bookId, int page, int size,
			Authentication connectedUser) {
		Pageable pageable = PageRequest.of(page, size);
        User user = ((User) connectedUser.getPrincipal());
        Page<Feedback> feedbacks = feedBackRepository.findAllByBookId(bookId, pageable);
        List<FeedbackResponse> feedbackResponses = feedbacks.stream()
                .map(f -> feedbackMapper.toFeedbackResponse(f, user.getId()))
                .toList();
        return new PageResponse<>(
                feedbackResponses,
                feedbacks.getNumber(),
                feedbacks.getSize(),
                feedbacks.getTotalElements(),
                feedbacks.getTotalPages(),
                feedbacks.isFirst(),
                feedbacks.isLast()
        );
	}

}
