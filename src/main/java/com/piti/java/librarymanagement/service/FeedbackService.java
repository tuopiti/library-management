package com.piti.java.librarymanagement.service;

import org.springframework.security.core.Authentication;

import com.piti.java.librarymanagement.dto.FeedbackRequest;
import com.piti.java.librarymanagement.dto.FeedbackResponse;
import com.piti.java.librarymanagement.dto.PageResponse;

public interface FeedbackService {
	Integer save(FeedbackRequest request, Authentication connectedUser);
	PageResponse<FeedbackResponse> findAllFeedbacksByBook(Integer bookId, int page, int size, Authentication connectedUser);
}
