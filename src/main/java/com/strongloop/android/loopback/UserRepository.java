package com.strongloop.android.loopback;

import java.util.HashMap;
import java.util.Map;

import org.json.JSONObject;

import com.strongloop.android.loopback.Model.Callback;
import com.strongloop.android.remoting.JsonUtil;
import com.strongloop.android.remoting.adapters.Adapter;
import com.strongloop.android.remoting.adapters.RestContract;
import com.strongloop.android.remoting.adapters.RestContractItem;

/**
 * Locally create or retrieves a user type from the server, encapsulating
 * the name of the user type for easy {@link User} creation, discovery, and
 * management.
 */
public class UserRepository extends ModelRepository<User> {
    
    private AccessTokenRepository accessTokenRepository;

    private RestAdapter getRestAdapter() {
        return (RestAdapter) getAdapter();
    }

    private AccessTokenRepository getAccessTokenRepository() {
        if (accessTokenRepository == null) {
            accessTokenRepository = getRestAdapter()
                    .createRepository(AccessTokenRepository.class);
        }
        return accessTokenRepository;
    }

    /**
     * Creates a new UserRepository, associating it with the static {@link User}
     * model class and the user class name.
     */
    public UserRepository() {
        super("user", User.class);
    }
    
    /**
     * Callback passed to loginUser to receive success and newly created
     * {@link User} instance or thrown error. 
     */
    public interface LoginCallback {
        public void onSuccess(AccessToken token, User currentUser);
        public void onError(Throwable t);
    }

    /**
     * Creates a {@link User} given an email and a password.
     * @param email
     * @param password
     * @return A {@link User}.
     */
    public User createUser(String email, String password)     {
        Map<String, Object> map = new HashMap<String, Object>();
        map.put("email", email);
        map.put("password", password);
        User user = createModel(map);
        return user;
    }
    
     /**
     * Creates a {@link RestContract} representing the user type's custom
     * routes. Used to extend an {@link Adapter} to support user. Calls
     * super {@link ModelRepository} createContract first.
     *
     * @return A {@link RestContract} for this model type.
     */
     
    public RestContract createContract() {
        RestContract contract = super.createContract();
        
        String className = getClassName();

        contract.addItem(new RestContractItem("/" + getNameForRestUrl() + "/login?include=user", "POST"),
                className + ".login");
        contract.addItem(new RestContractItem("/" + getNameForRestUrl() + "/logout", "GET"),
                className + ".logout");
        return contract;
    }
    
    /**
     * Creates a new {@link User} given the email, password and optional parameters.
     * @param email - user email
     * @param password - user password
     * @param parameters - optional parameters
     * @return A new {@link User}
     */
    public User createUser(String email, String password, 
            Map<String, ? extends Object> parameters) {

        HashMap<String, Object> allParams = new HashMap<String, Object>();
        allParams.putAll(parameters);
        allParams.put("email", email);
        allParams.put("password", password);
        User user = createModel(allParams);
        
        return user;    
    }
        
    /**
     * Login a user given an email and password. Creates a User model if successful.
     * @param email - user email
     * @param password - user password
     * @param callback - success/error callback
     */
    public void loginUser(String email, String password, 
            final LoginCallback callback) {
        
        HashMap<String, Object> params = new HashMap<String, Object>();
        params.put("email",  email);
        params.put("password",  password);

        invokeStaticMethod("login", params,
                new Adapter.JsonObjectCallback() {

                    @Override
                    public void onError(Throwable t) {
                        callback.onError(t);
                    }

                    @Override
                    public void onSuccess(JSONObject response) {
                        AccessToken token = getAccessTokenRepository()
                                .createModel(JsonUtil.fromJson(response));
                        getRestAdapter().setAccessToken(token.getId().toString());

                        JSONObject userJson = response.optJSONObject("user");
                        User user = userJson != null
                                ? createModel(JsonUtil.fromJson(userJson))
                                : null;

                        callback.onSuccess(token, user);
                    }
                });
    }

    /**
     * Logs the current User out of the server and removes the access
     * token from the system.
     * <p>
     * @param callback The callback to be executed when finished.
     */
     
    public void logout(final Callback callback) {
        
        invokeStaticMethod("logout", null,
                new Adapter.Callback() {

            @Override
            public void onError(Throwable t) {
                callback.onError(t);
            }

            @Override
            public void onSuccess(String response) {
                RestAdapter radapter = (RestAdapter)getAdapter();
                radapter.clearAccessToken();
                callback.onSuccess();
            }
        });
    }
    
}