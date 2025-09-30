package com.cheers.office.board.controller;

import java.util.Optional;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.cheers.office.board.model.CustomUserDetails; // CustomUserDetailsのimport
import com.cheers.office.board.model.Post;
import com.cheers.office.board.model.Thread;
import com.cheers.office.board.service.BoardService;

@Controller
@RequestMapping("/board")
public class BoardController {

    private final BoardService boardService;

    public BoardController(BoardService boardService) {
        this.boardService = boardService;
    }

    // 全スレッド表示
    @GetMapping // /board
    public String getAllThreads(Model model) {
        model.addAttribute("threads", boardService.findAllThreads()); // ★変更: getAllPosts() -> findAllThreads()
        model.addAttribute("newThreadForm", new Thread()); // 新規スレッド作成フォーム用
        return "board/list"; // board/list.html を返す
    }

    // スレッド詳細表示
    @GetMapping("/{threadId}") // /board/{threadId}
    public String getThreadById(@PathVariable String threadId, Model model) {
        Optional<Thread> thread = boardService.findThreadById(threadId); // ★変更: getPostById() -> findThreadById()
        if (thread.isPresent()) {
            model.addAttribute("thread", thread.get());
            model.addAttribute("newPostForm", new Post()); // 新規投稿フォーム用
            return "board/detail"; // board/detail.html を返す
        }
        return "redirect:/board"; // スレッドが見つからなければリストに戻る
    }

    // 新規スレッド作成
    @PostMapping("/create") // /board/create
    public String createNewThread(
            @ModelAttribute("newThreadForm") Thread newThreadForm,
            @AuthenticationPrincipal CustomUserDetails userDetails,
            RedirectAttributes redirectAttributes) {

        String userId = (userDetails != null) ? userDetails.getUser().getUserId() : "anonymous";
        
        // ★変更: createPost() -> createNewThread() を呼び出し、引数もスレッドタイトルと最初の投稿内容に合わせる
        // newThreadFormのpostsリストから最初の投稿内容を取得
        String firstPostContent = "";
        if (newThreadForm.getPosts() != null && !newThreadForm.getPosts().isEmpty()) {
            firstPostContent = newThreadForm.getPosts().get(0).getContent();
        }
        boardService.createNewThread(newThreadForm.getTitle(), firstPostContent, userId);
        
        redirectAttributes.addFlashAttribute("message", "新しいスレッドが作成されました！");
        return "redirect:/board";
    }

    // スレッドに投稿を追加
    @PostMapping("/{threadId}/addPost") // /board/{threadId}/addPost
    public String addPostToThread(
            @PathVariable String threadId,
            @ModelAttribute("newPostForm") Post newPostForm,
            @AuthenticationPrincipal CustomUserDetails userDetails,
            RedirectAttributes redirectAttributes) {

        String userId = (userDetails != null) ? userDetails.getUser().getUserId() : "anonymous";
        boardService.addPostToThread(threadId, newPostForm.getContent(), userId); // ★変更なし (addPostToThreadはすでに存在)
        redirectAttributes.addFlashAttribute("message", "投稿が追加されました！");
        return "redirect:/board/" + threadId;
    }

    // スレッド削除 (管理者や作成者のみが実行できるようにする必要がある)
    @PostMapping("/{threadId}/delete") // /board/{threadId}/delete
    public String deleteThread(@PathVariable String threadId, RedirectAttributes redirectAttributes) {
        // ★変更: deletePost() -> deleteThread() (もしBoardServiceにdeleteThreadメソッドがあれば)
        // 現在のBoardServiceにはdeleteThreadがないので、ここではコメントアウトしておくか、後でBoardServiceに追加する
        // boardService.deleteThread(threadId); 
        redirectAttributes.addFlashAttribute("message", "スレッドは削除されました (※この機能は現在ダミーです)。");
        return "redirect:/board";
    }

    // updatePost メソッドはBoardServiceに存在しないため、BoardControllerからも削除またはコメントアウト
    /*
    @PostMapping("/update")
    public String updatePost(@ModelAttribute Post post, RedirectAttributes redirectAttributes) {
        // boardService.updatePost(post); // BoardServiceにupdatePostが存在しない
        redirectAttributes.addFlashAttribute("message", "投稿が更新されました！");
        return "redirect:/board";
    }
    */
}