/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package controller.web;

import model.UserGoogleDto;
import model.Constants;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import dal.UserDAO;
import java.io.IOException;
import java.io.PrintWriter;
import java.sql.ResultSet;
import javax.servlet.RequestDispatcher;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import model.UserDTO;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.fluent.Request;
import org.apache.http.client.fluent.Form;

/**
 *
 * @author HuuThanh
 */
@WebServlet(name = "LoginServlet", urlPatterns = {"/LoginServlet"})
public class LoginServlet extends HttpServlet {

    private final String WELCOME = "home.jsp";
    private final String LOGIN = "login.jsp";
    private final String ADMIN_DASHBOARD = "AdminServlet";

    protected void processRequest(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        String code = request.getParameter("code");
        String accessToken = getToken(code);
        UserGoogleDto user = getUserInfo(accessToken);
        System.out.println(user);
    }

    public static String getToken(String code) throws ClientProtocolException, IOException {
        // call api to get token
        String response = Request.Post(Constants.GOOGLE_LINK_GET_TOKEN)
                .bodyForm(Form.form().add("client_id", Constants.GOOGLE_CLIENT_ID)
                        .add("client_secret", Constants.GOOGLE_CLIENT_SECRET)
                        .add("redirect_uri", Constants.GOOGLE_REDIRECT_URI).add("code", code)
                        .add("grant_type", Constants.GOOGLE_GRANT_TYPE).build())
                .execute().returnContent().asString();

        JsonObject jobj = new Gson().fromJson(response, JsonObject.class);
        String accessToken = jobj.get("access_token").toString().replaceAll("\"", "");
        return accessToken;
    }

    public static UserGoogleDto getUserInfo(final String accessToken) throws ClientProtocolException, IOException {
        String link = Constants.GOOGLE_LINK_GET_USER_INFO + accessToken;
        String response = Request.Get(link).execute().returnContent().asString();

        UserGoogleDto googlePojo = new Gson().fromJson(response, UserGoogleDto.class);

        return googlePojo;
    }

    // <editor-fold defaultstate="collapsed" desc="HttpServlet methods. Click on the +
    // sign on the left to edit the code.">
    /**
     * Handles the HTTP <code>GET</code> method.
     *
     * @param request servlet request
     * @param response servlet response
     * @throws ServletException if a servlet-specific error occurs
     * @throws IOException if an I/O error occurs
     */
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        if (request.getParameter("action") != null) {
            Cookie arr[] = request.getCookies();
            if (arr != null) {
                for (int i = 0; i < arr.length; i++) {
                    if (arr[i].getName().equals("cUName")) {
                        request.setAttribute("uName", arr[i].getValue());
                    }
                    if (arr[i].getName().equals("cUPass")) {
                        request.setAttribute("uPass", arr[i].getValue());
                    }
                    if (arr[i].getName().equals("reMem")) {
                        request.setAttribute("reMem", arr[i].getValue());
                    }
                }
            }
            request.getRequestDispatcher(LOGIN).forward(request, response);
        } else {
            processRequest(request, response);
            request.getRequestDispatcher(WELCOME).forward(request, response);
        }
    }

    /**
     * Handles the HTTP <code>POST</code> method.
     *
     * @param request servlet request
     * @param response servlet response
     * @throws ServletException if a servlet-specific error occurs
     * @throws IOException if an I/O error occurs
     */
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        response.setContentType("text/html;charset=UTF-8");
        PrintWriter out = response.getWriter();
        String url = WELCOME;
        ResultSet rs = null;
        try {
            String btnValue = request.getParameter("btnAction");
            String username = request.getParameter("txtUsername");
            String password = request.getParameter("txtPassword");
            String remember = request.getParameter("remember");
            UserDAO dao = new UserDAO();
            UserDTO user = dao.checkLogin(username, password);
            if (user != null) {
                HttpSession session = request.getSession();
                session.setAttribute("account", user);

                Cookie u = new Cookie("cUName", username);
                Cookie p = new Cookie("cUPass", password);
                Cookie r = new Cookie("reMem", remember);

                u.setMaxAge(60 * 60 * 24 * 30 * 3); //3months
                if (remember != null) {
                    p.setMaxAge(60 * 60 * 24 * 30 * 3);
                    r.setMaxAge(60 * 60 * 24 * 30 * 3);
                } else {
                    p.setMaxAge(0);
                    r.setMaxAge(0);
                }

                response.addCookie(u);
                response.addCookie(p);
                response.addCookie(r);
                if (dao.checkAdmin(user)) {
                    response.sendRedirect(ADMIN_DASHBOARD);
                } else {
                    response.sendRedirect(WELCOME);

                }
            } else {
                String error = "Invalid username or password!";
                request.setAttribute("msg", error);
                RequestDispatcher rd = request.getRequestDispatcher(LOGIN);
                rd.forward(request, response);
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            out.close();
        }
    }

    /**
     * Returns a short description of the servlet.
     *
     * @return a String containing servlet description
     */
    @Override
    public String getServletInfo() {
        return "Short description";
    }// </editor-fold>

}
