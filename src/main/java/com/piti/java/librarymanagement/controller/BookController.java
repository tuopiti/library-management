package com.piti.java.librarymanagement.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.piti.java.librarymanagement.dto.BookRequest;
import com.piti.java.librarymanagement.dto.BookResponse;
import com.piti.java.librarymanagement.dto.BorrowedBookResponse;
import com.piti.java.librarymanagement.dto.PageResponse;
import com.piti.java.librarymanagement.service.BookService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("books")
@RequiredArgsConstructor
//@Tag(name = "Book")
public class BookController {

	private final BookService bookService;
	
	@PostMapping
    public ResponseEntity<Integer> saveBook(@Valid @RequestBody BookRequest bookRequest, Authentication connectedUser) {
        return ResponseEntity.ok(bookService.save(bookRequest, connectedUser));
    }
	
	@GetMapping("/{book-id}")
	public ResponseEntity<BookResponse> findBookById(@PathVariable("book-id") Integer bookId) {
	    return ResponseEntity.ok(bookService.findById(bookId));
	}
	
	@GetMapping
    public ResponseEntity<PageResponse<BookResponse>> findAllBooks(
            @RequestParam(name = "page", defaultValue = "0", required = false) int page,
            @RequestParam(name = "size", defaultValue = "10", required = false) int size,
            Authentication connectedUser) {
		
        return ResponseEntity.ok(bookService.findAllBooks(page, size, connectedUser));
    }
	
	
    @GetMapping("/owner")
    public ResponseEntity<PageResponse<BookResponse>> findAllBooksByOwner(
            @RequestParam(name = "page", defaultValue = "0", required = false) int page,
            @RequestParam(name = "size", defaultValue = "10", required = false) int size,
            Authentication connectedUser) {
    	
        return ResponseEntity.ok(bookService.findAllBooksByOwner(page, size, connectedUser));
    }
    
    
    @GetMapping("/borrowed")
    public ResponseEntity<PageResponse<BorrowedBookResponse>> findAllBorrowedBooks(
            @RequestParam(name = "page", defaultValue = "0", required = false) int page,
            @RequestParam(name = "size", defaultValue = "10", required = false) int size,
            Authentication connectedUser) {
    	
        return ResponseEntity.ok(bookService.findAllBorrowedBooks(page, size, connectedUser));
    }
    
    
    @GetMapping("/returned")
    public ResponseEntity<PageResponse<BorrowedBookResponse>> findAllReturnedBooks(
            @RequestParam(name = "page", defaultValue = "0", required = false) int page,
            @RequestParam(name = "size", defaultValue = "10", required = false) int size,
            Authentication connectedUser) {
    	
        return ResponseEntity.ok(bookService.findAllReturnedBooks(page, size, connectedUser));
    }
    
    
    @PatchMapping("/shareable/{book-id}")
    public ResponseEntity<Integer> updateShareableStatus(
            @PathVariable("book-id") Integer bookId,
            Authentication connectedUser) {
    	
        return ResponseEntity.ok(bookService.updateShareableStatus(bookId, connectedUser));
    }
    
   
    @PatchMapping("/archived/{book-id}")
    public ResponseEntity<Integer> updateArchivedStatus(
            @PathVariable("book-id") Integer bookId,
            Authentication connectedUser) {
    	
        return ResponseEntity.ok(bookService.updateArchivedStatus(bookId, connectedUser));
    }
    
    
    @PostMapping("borrow/{book-id}")
    public ResponseEntity<Integer> borrowBook(
            @PathVariable("book-id") Integer bookId,
            Authentication connectedUser) {
    	
        return ResponseEntity.ok(bookService.borrowBook(bookId, connectedUser));
    }
    
    
    @PatchMapping("borrow/return/{book-id}")
    public ResponseEntity<Integer> returnBorrowBook(
            @PathVariable("book-id") Integer bookId,
            Authentication connectedUser) {
    	
        return ResponseEntity.ok(bookService.returnBorrowedBook(bookId, connectedUser));
    }
    
    
    @PatchMapping("borrow/return/approve/{book-id}")
    public ResponseEntity<Integer> approveReturnBorrowBook(
            @PathVariable("book-id") Integer bookId,
            Authentication connectedUser) {
    	
        return ResponseEntity.ok(bookService.approveReturnBorrowedBook(bookId, connectedUser));
    }
    
    
    @PostMapping(value = "/cover/{book-id}", consumes = "multipart/form-data")
    public ResponseEntity<?> uploadBookCoverPicture(
            @PathVariable("book-id") Integer bookId,
//            @Parameter()
            @RequestPart("file") MultipartFile file,
            Authentication connectedUser) {
    	
    	bookService.uploadBookCoverPicture(file, connectedUser, bookId);
        return ResponseEntity.accepted().build();
    }
    
}
