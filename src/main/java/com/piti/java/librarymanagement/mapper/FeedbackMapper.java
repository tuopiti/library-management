package com.piti.java.librarymanagement.mapper;

import java.util.Objects;

import org.springframework.stereotype.Service;

import com.piti.java.librarymanagement.dto.FeedbackRequest;
import com.piti.java.librarymanagement.dto.FeedbackResponse;
import com.piti.java.librarymanagement.model.Book;
import com.piti.java.librarymanagement.model.Feedback;

@Service
public class FeedbackMapper {
    public Feedback toFeedback(FeedbackRequest request) {
        return Feedback.builder()
                .note(request.note())
                .comment(request.comment())
                .book(Book.builder()
                        .id(request.bookId())
                        .shareable(false) // Not required and has no impact :: just to satisfy lombok
                        .archived(false) // Not required and has no impact :: just to satisfy lombok
                        .build()
                )
                .build();
    }

    public FeedbackResponse toFeedbackResponse(Feedback feedback, Integer id) {
        return FeedbackResponse.builder()
                .note(feedback.getNote())
                .comment(feedback.getComment())
                .ownFeedback(Objects.equals(feedback.getCreatedBy(), id))
                .build();
    }
}
