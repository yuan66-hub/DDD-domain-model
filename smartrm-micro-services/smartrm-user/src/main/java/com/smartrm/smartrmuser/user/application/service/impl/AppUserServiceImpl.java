package com.smartrm.smartrmuser.user.application.service.impl;

import com.google.common.collect.Maps;
import com.smartrm.infracore.exception.DomainException;
import com.smartrm.infracore.security.Authority;
import com.smartrm.infracore.security.JwtUtil;
import com.smartrm.smartrmuser.user.application.client.WxPlatformClient;
import com.smartrm.smartrmuser.user.application.dto.PaymentContractInfoDto;
import com.smartrm.smartrmuser.user.application.dto.SignInCommandDto;
import com.smartrm.smartrmuser.user.application.dto.SignInResultCode;
import com.smartrm.smartrmuser.user.application.dto.SignInResultDto;
import com.smartrm.smartrmuser.user.application.dto.UserInfoDto;
import com.smartrm.smartrmuser.user.application.dto.UserInfoQueryDto;
import com.smartrm.smartrmuser.user.application.dto.WxCode2SessionResultDto;
import com.smartrm.smartrmuser.user.application.service.AppUserService;
import com.smartrm.smartrmuser.user.domain.UserAccount;
import com.smartrm.smartrmuser.user.domain.repository.UserAccountRepository;
import com.smartrm.smartrmuser.user.infrastructure.UserError;
import java.util.Map;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

/**
 * @author: yoda
 * @description:
 */
@Service
public class AppUserServiceImpl implements AppUserService {

  private Logger LOGGER = LoggerFactory.getLogger(AppUserServiceImpl.class);

  @Autowired
  @Qualifier("mockWxPlatformClientImpl")
  WxPlatformClient wxPlatformClient;

  @Autowired
  UserAccountRepository userAccountRepository;

  @Autowired
  JwtUtil jwtUtil;

  @Override
  public SignInResultDto signInOrSignUp(SignInCommandDto cmd) {
    WxCode2SessionResultDto wxCode2SessionResult = wxPlatformClient.code2Session(cmd.getWxJsCode());
    if (wxCode2SessionResult.getErrcode() != 0) {
      LOGGER.error("fail to login weixin:" + cmd.getWxJsCode(), wxCode2SessionResult.getErrmsg());
      throw new DomainException(UserError.FailToLoginWx);
    }

    UserAccount account = userAccountRepository.getByOpenId(wxCode2SessionResult.getOpenId());
    if (account == null) {
      //新建账号
      UserAccount.Builder builder = UserAccount.Builder()
          .wxOpenId(wxCode2SessionResult.getOpenId());
      if (!StringUtils.isEmpty(wxCode2SessionResult.getUnionId())) {
        builder.wxUnionId(wxCode2SessionResult.getUnionId());
      }
      account = builder.build();
      userAccountRepository.add(account);
    }

    SignInResultDto ret = new SignInResultDto();
    if (StringUtils.isEmpty(account.getContractId())) {
      ret.setResult(SignInResultCode.NeedSignContract);
      return ret;
    }
    Map<String, Object> claims = Maps.newHashMap();
    claims.put("authorities", Authority.OpenCabinet.name());
    ret.setToken(jwtUtil.createToken(account.getWxOpenId(), claims));
    ret.setResult(SignInResultCode.Success);
    return ret;
  }

  @Override
  public UserInfoDto getUserInfo(UserInfoQueryDto query) {
    UserAccount account = userAccountRepository.getByOpenId(query.getOpenId());
    if (account == null) {
      return null;
    }
    UserInfoDto ret = new UserInfoDto();
    ret.setAccountId(account.getAccountId());
    ret.setContractId(account.getContractId());
    ret.setWxOpenId(account.getWxOpenId());
    ret.setWxUnionId(account.getWxUnionId());
    return ret;
  }

  @Override
  public void onPaymentContractSigned(PaymentContractInfoDto dto) {
    UserAccount account = userAccountRepository.getByOpenId(dto.getOpenId());
    account.setContractId(dto.getContractId());
    userAccountRepository.update(account);
  }

}
