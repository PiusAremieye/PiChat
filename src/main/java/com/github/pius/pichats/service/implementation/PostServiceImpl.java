package com.github.pius.pichats.service.implementation;

import com.github.pius.pichats.exceptions.CustomException;
import com.github.pius.pichats.model.Post;
import com.github.pius.pichats.model.User;
import com.github.pius.pichats.repository.PostRepository;
import com.github.pius.pichats.security.JwtProvider;
import com.github.pius.pichats.service.PostService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import javax.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.List;

@Service
public class PostServiceImpl implements PostService {
  private JwtProvider jwtProvider;
  private PostRepository postRepository;
  private CloudService cloudService;
  private List<String> listOfPosts = new ArrayList<>();

  @Autowired
  public PostServiceImpl(JwtProvider jwtProvider, PostRepository postRepository, CloudService cloudService) {
    this.jwtProvider = jwtProvider;
    this.postRepository = postRepository;
    this.cloudService = cloudService;
  }

  // random post to comment on by any user
  @Override
  public Post getPost(String post) {
    try{
      return postRepository.findByPost(post).orElseThrow(() -> new CustomException("Post : "+post+ " does not exists", HttpStatus.NOT_FOUND));
    }catch (Exception ex){
      throw new CustomException(ex.getMessage(), HttpStatus.NOT_FOUND);
    }
  }

  // a logged in user makes a post
  @Override
  public Post post(Post post, HttpServletRequest request) {
    try{
      User user = jwtProvider.resolveUser(request);
      Post newPost = new Post();
      newPost.setCaption(post.getCaption());
      // upload post if username exists
      cloudService.upload(post.getPost());
      newPost.setPost(cloudService.fileName);
      newPost.setUser(user);
      return postRepository.save(newPost);
    }catch (Exception ex){
      throw new CustomException(ex.getMessage(), HttpStatus.NOT_FOUND);
    }
  }

  // find a particular post of a logged in user
  @Override
  public Post findPost(String post, HttpServletRequest request) {
    try{
      User user = jwtProvider.resolveUser(request);
      Post postFound = this.getPost(post);
      if (postFound.getUser().equals(user)){
        return postFound;
      }
      throw new CustomException(user.getUsername()+ " does not have post "+post, HttpStatus.NOT_FOUND);
    }catch (Exception ex){
      throw new CustomException(ex.getMessage(), HttpStatus.NOT_FOUND);
    }
  }

  // return all the post of a logged in user
  @Override
  public List<Post> findAll(HttpServletRequest request) {
    try{
      User user = jwtProvider.resolveUser(request);
      return postRepository.findAllByUserOrderByCreatedAt(user);
    }catch (Exception ex){
      throw new CustomException(ex.getMessage(), HttpStatus.NOT_FOUND);
    }
  }

  // find a post of the logged in user and delete
  @Override
  public void delete(String post, HttpServletRequest request) throws Exception {
    cloudService.deleteFile(post);
    postRepository.delete(this.findPost(post, request));
  }

  // find a post of the logged in user mark put in a list
  @Override
  public String selectPostToDelete(String post, HttpServletRequest request){
    Post postFound = this.findPost(post, request);
    if (!listOfPosts.contains(postFound.getPost())){
      listOfPosts.add(postFound.getPost());
      return "Post : "+post+" have been marked";
    }
    listOfPosts.remove(postFound.getPost());
    return "Post : "+post+" have been unmarked";
  }

  // clear the list of posts if not empty
  @Override
  public String clearBatchDelete(HttpServletRequest request){
    if (!listOfPosts.isEmpty()){
      listOfPosts.clear();
      return "Post(s) marked for delete have been canceled";
    }
    return "Post(s) marked for delete is empty";
  }

  // delete the list posts of a logged in user
  @Override
  public String batchDelete(HttpServletRequest request) throws Exception {
    if (!listOfPosts.isEmpty()){
      for (String post : listOfPosts){
        delete(post, request);
      }
      listOfPosts.clear();
      return "Posts marked have been deleted";
    }
    return "There is no post(s) marked to delete";
  }

  @Override
  public int countPostsOfUser(HttpServletRequest request) {
    try{
      User user = jwtProvider.resolveUser(request);
      return postRepository.countPostsByUser(user);
    }catch (Exception ex){
      throw new CustomException(ex.getMessage(), HttpStatus.NOT_FOUND);
    }
  }

}