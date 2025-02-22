package com.piti.java.librarymanagement.service.impl;

import java.util.List;
import java.util.Objects;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.piti.java.librarymanagement.dto.BookRequest;
import com.piti.java.librarymanagement.dto.BookResponse;
import com.piti.java.librarymanagement.dto.BorrowedBookResponse;
import com.piti.java.librarymanagement.dto.PageResponse;
import com.piti.java.librarymanagement.exception.OperationNotPermittedException;
import com.piti.java.librarymanagement.file.FileStorageService;
import com.piti.java.librarymanagement.mapper.BookMapper;
import com.piti.java.librarymanagement.model.Book;
import com.piti.java.librarymanagement.model.BookTransactionHistory;
import com.piti.java.librarymanagement.model.User;
import com.piti.java.librarymanagement.repository.BookRepository;
import com.piti.java.librarymanagement.repository.BookTransactionHistoryRepository;
import com.piti.java.librarymanagement.service.BookService;

import jakarta.persistence.EntityNotFoundException;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import static com.piti.java.librarymanagement.spec.BookSpecification.withOwnerId;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class BookServiceImpl implements BookService{
	private final BookRepository bookRepository;
    private final BookMapper bookMapper;
    private final BookTransactionHistoryRepository transactionHistoryRepository;
    private final FileStorageService fileStorageService;

	@Override
	public Integer save(BookRequest request, Authentication connectedUser) {
		User user = ((User) connectedUser.getPrincipal());
        Book book = bookMapper.toBook(request);
        book.setOwner(user);
        return bookRepository.save(book).getId();
	}
	

	@Override
	public BookResponse findById(Integer bookId) {
		return bookRepository.findById(bookId)
                .map(bookMapper::toBookResponse)
                .orElseThrow(() -> new EntityNotFoundException("No book found with ID:: " + bookId));
	}
	

	@Override
	public PageResponse<BookResponse> findAllBooks(int page, int size, Authentication connectedUser) {
		User user = ((User) connectedUser.getPrincipal());
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdDate").descending());
        Page<Book> books = bookRepository.findAllDisplayableBooks(pageable, user.getId());
        List<BookResponse> booksResponse = books.stream()
                .map(bookMapper::toBookResponse)
                .toList();
        return new PageResponse<>(
                booksResponse,
                books.getNumber(),
                books.getSize(),
                books.getTotalElements(),
                books.getTotalPages(),
                books.isFirst(),
                books.isLast()
        );
	}


	@Override
	public PageResponse<BookResponse> findAllBooksByOwner(int page, int size, Authentication connectedUser) {
		 User user = ((User) connectedUser.getPrincipal());
	        Pageable pageable = PageRequest.of(page, size, Sort.by("createdDate").descending());
	        Page<Book> books = bookRepository.findAll(withOwnerId(user.getId()), pageable);
	        List<BookResponse> booksResponse = books.stream()
	                .map(bookMapper::toBookResponse)
	                .toList();
	        return new PageResponse<>(
	                booksResponse,
	                books.getNumber(),
	                books.getSize(),
	                books.getTotalElements(),
	                books.getTotalPages(),
	                books.isFirst(),
	                books.isLast()
	        );
	}


	@Override
	public Integer updateShareableStatus(Integer bookId, Authentication connectedUser) {
		Book book = bookRepository.findById(bookId)
                .orElseThrow(() -> new EntityNotFoundException("No book found with ID:: " + bookId));
        User user = ((User) connectedUser.getPrincipal());
        if (!Objects.equals(book.getOwner().getId(), user.getId())) {
            throw new OperationNotPermittedException("You cannot update others books shareable status");
        }
        book.setShareable(!book.isShareable());
        bookRepository.save(book);
        return bookId;
	}
	


	@Override
	public Integer updateArchivedStatus(Integer bookId, Authentication connectedUser) {
		Book book = bookRepository.findById(bookId)
                .orElseThrow(() -> new EntityNotFoundException("No book found with ID:: " + bookId));
        User user = ((User) connectedUser.getPrincipal());
        if (!Objects.equals(book.getOwner().getId(), user.getId())) {
            throw new OperationNotPermittedException("You cannot update others books archived status");
        }
        book.setArchived(!book.isArchived());
        bookRepository.save(book);
        return bookId;
	}


	@Override
	public Integer borrowBook(Integer bookId, Authentication connectedUser) {
		  Book book = bookRepository.findById(bookId)
	                .orElseThrow(() -> new EntityNotFoundException("No book found with ID:: " + bookId));
	        if (book.isArchived() || !book.isShareable()) {
	            throw new OperationNotPermittedException("The requested book cannot be borrowed since it is archived or not shareable");
	        }
	        User user = ((User) connectedUser.getPrincipal());
	        if (Objects.equals(book.getOwner().getId(), user.getId())) {
	            throw new OperationNotPermittedException("You cannot borrow your own book");
	        }
	        final boolean isAlreadyBorrowedByUser = transactionHistoryRepository.isAlreadyBorrowedByUser(bookId, user.getId());
	        if (isAlreadyBorrowedByUser) {
	            throw new OperationNotPermittedException("You already borrowed this book and it is still not returned or the return is not approved by the owner");
	        }

	        final boolean isAlreadyBorrowedByOtherUser = transactionHistoryRepository.isAlreadyBorrowed(bookId);
	        if (isAlreadyBorrowedByOtherUser) {
	            throw new OperationNotPermittedException("Te requested book is already borrowed");
	        }

	        BookTransactionHistory bookTransactionHistory = BookTransactionHistory.builder()
	                .user(user)
	                .book(book)
	                .returned(false)
	                .returnApproved(false)
	                .build();
	        return transactionHistoryRepository.save(bookTransactionHistory).getId();

	}


	@Override
	public Integer returnBorrowedBook(Integer bookId, Authentication connectedUser) {
		Book book = bookRepository.findById(bookId)
                .orElseThrow(() -> new EntityNotFoundException("No book found with ID:: " + bookId));
        if (book.isArchived() || !book.isShareable()) {
            throw new OperationNotPermittedException("The requested book is archived or not shareable");
        }
        User user = ((User) connectedUser.getPrincipal());
        if (Objects.equals(book.getOwner().getId(), user.getId())) {
            throw new OperationNotPermittedException("You cannot borrow or return your own book");
        }

        BookTransactionHistory bookTransactionHistory = transactionHistoryRepository.findByBookIdAndUserId(bookId, user.getId())
                .orElseThrow(() -> new OperationNotPermittedException("You did not borrow this book"));

        bookTransactionHistory.setReturned(true);
        return transactionHistoryRepository.save(bookTransactionHistory).getId();
	}


	@Override
	public Integer approveReturnBorrowedBook(Integer bookId, Authentication connectedUser) {
		Book book = bookRepository.findById(bookId)
	                .orElseThrow(() -> new EntityNotFoundException("No book found with ID:: " + bookId));
        if (book.isArchived() || !book.isShareable()) {
            throw new OperationNotPermittedException("The requested book is archived or not shareable");
        }
        User user = ((User) connectedUser.getPrincipal());
        if (!Objects.equals(book.getOwner().getId(), user.getId())) {
            throw new OperationNotPermittedException("You cannot approve the return of a book you do not own");
        }

        BookTransactionHistory bookTransactionHistory = transactionHistoryRepository.findByBookIdAndOwnerId(bookId, user.getId())
                .orElseThrow(() -> new OperationNotPermittedException("The book is not returned yet. You cannot approve its return"));

        bookTransactionHistory.setReturnApproved(true);
        return transactionHistoryRepository.save(bookTransactionHistory).getId();
	}


	@Override
	public void uploadBookCoverPicture(MultipartFile file, Authentication connectedUser, Integer bookId) {
		Book book = bookRepository.findById(bookId)
                .orElseThrow(() -> new EntityNotFoundException("No book found with ID:: " + bookId));
        User user = ((User) connectedUser.getPrincipal());
        var profilePicture = fileStorageService.saveFile(file, bookId, user.getId());
        book.setBookCover(profilePicture);
        bookRepository.save(book);
		
	}


	@Override
	public PageResponse<BorrowedBookResponse> findAllBorrowedBooks(int page, int size, Authentication connectedUser) {
		User user = ((User) connectedUser.getPrincipal());
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdDate").descending());
        Page<BookTransactionHistory> allBorrowedBooks = transactionHistoryRepository.findAllBorrowedBooks(pageable, user.getId());
        List<BorrowedBookResponse> booksResponse = allBorrowedBooks.stream()
                .map(bookMapper::toBorrowedBookResponse)
                .toList();
        return new PageResponse<>(
                booksResponse,
                allBorrowedBooks.getNumber(),
                allBorrowedBooks.getSize(),
                allBorrowedBooks.getTotalElements(),
                allBorrowedBooks.getTotalPages(),
                allBorrowedBooks.isFirst(),
                allBorrowedBooks.isLast()
        );
	}


	@Override
	public PageResponse<BorrowedBookResponse> findAllReturnedBooks(int page, int size, Authentication connectedUser) {
		User user = ((User) connectedUser.getPrincipal());
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdDate").descending());
        Page<BookTransactionHistory> allBorrowedBooks = transactionHistoryRepository.findAllReturnedBooks(pageable, user.getId());
        List<BorrowedBookResponse> booksResponse = allBorrowedBooks.stream()
                .map(bookMapper::toBorrowedBookResponse)
                .toList();
        return new PageResponse<>(
                booksResponse,
                allBorrowedBooks.getNumber(),
                allBorrowedBooks.getSize(),
                allBorrowedBooks.getTotalElements(),
                allBorrowedBooks.getTotalPages(),
                allBorrowedBooks.isFirst(),
                allBorrowedBooks.isLast()
        );
	}
	
	

}
