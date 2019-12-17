/*
 * Waltz - Enterprise Architecture
 * Copyright (C) 2016, 2017, 2018, 2019 Waltz open source project
 * See README.md for more information
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.khartec.waltz.service.end_user_app;

import com.khartec.waltz.common.DateTimeUtilities;
import com.khartec.waltz.data.application.ApplicationDao;
import com.khartec.waltz.data.changelog.ChangeLogDao;
import com.khartec.waltz.data.end_user_app.EndUserAppDao;
import com.khartec.waltz.data.end_user_app.EndUserAppIdSelectorFactory;
import com.khartec.waltz.data.orgunit.OrganisationalUnitIdSelectorFactory;
import com.khartec.waltz.model.*;
import com.khartec.waltz.model.application.AppRegistrationRequest;
import com.khartec.waltz.model.application.AppRegistrationResponse;
import com.khartec.waltz.model.application.ApplicationKind;
import com.khartec.waltz.model.application.ImmutableAppRegistrationRequest;
import com.khartec.waltz.model.changelog.ChangeLog;
import com.khartec.waltz.model.changelog.ImmutableChangeLog;
import com.khartec.waltz.model.enduserapp.EndUserApplication;
import com.khartec.waltz.model.rating.RagRating;
import com.khartec.waltz.model.tally.Tally;
import org.jooq.Record1;
import org.jooq.Select;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.List;

import static com.khartec.waltz.common.Checks.checkNotNull;
import static com.khartec.waltz.common.FunctionUtilities.time;
import static com.khartec.waltz.model.EntityKind.APPLICATION;

@Service
public class EndUserAppService {

    private final EndUserAppDao endUserAppDao;
    private final ApplicationDao applicationDao;
    private final ChangeLogDao changeLogDao;
    private final EndUserAppIdSelectorFactory endUserAppIdSelectorFactory = new EndUserAppIdSelectorFactory();
    private final OrganisationalUnitIdSelectorFactory orgUnitIdSelectorFactory= new OrganisationalUnitIdSelectorFactory();


    @Autowired
    public EndUserAppService(EndUserAppDao endUserAppDao,
                             ApplicationDao applicationDao,
                             ChangeLogDao changeLogDao) {
        checkNotNull(endUserAppDao, "EndUserAppDao is required");
        checkNotNull(applicationDao, "ApplicationDao is required");
        checkNotNull(changeLogDao, "ChangeLogDao is required");
        this.endUserAppDao = endUserAppDao;
        this.applicationDao = applicationDao;
        this.changeLogDao = changeLogDao;
    }


    @Deprecated
    public List<EndUserApplication> findByOrganisationalUnitSelector(IdSelectionOptions options) {
        checkNotNull(options, "options cannot be null");
        Select<Record1<Long>> selector = orgUnitIdSelectorFactory.apply(options);
        return time("EUAS.findByOrganisationalUnitSelector", () -> endUserAppDao.findByOrganisationalUnitSelector(selector));
    }


    public Collection<Tally<Long>> countByOrgUnitId() {
        return time("EUAS.countByOrgUnitId", () -> endUserAppDao.countByOrganisationalUnit());
    }


    public List<EndUserApplication> findBySelector(IdSelectionOptions options) {
        checkNotNull(options, "options cannot be null");
        Select<Record1<Long>> selector = endUserAppIdSelectorFactory.apply(options);
        return time("EUAS.findBySelector", () -> endUserAppDao.findBySelector(selector));
    }


    public List<EndUserApplication> findAll() {
        return endUserAppDao.findAll();
    }


    public AppRegistrationResponse promoteToApplication(Long id, String username){

        AppRegistrationRequest appRegistrationRequest = createAppRegistrationRequest(id);

        checkNotNull(appRegistrationRequest, "EUDA has already been promoted");

        endUserAppDao.updateIsPromotedFlag(id);

        AppRegistrationResponse appRegistrationResponse = applicationDao.registerApp(appRegistrationRequest);

        changeLogDao.write(mkChangeLog(appRegistrationResponse, username));

        return appRegistrationResponse;
    }


    private ChangeLog mkChangeLog(AppRegistrationResponse appRegistrationResponse, String username) {
        return ImmutableChangeLog.builder()
                .message(String.format("Promoted application '%s' from an end user application", appRegistrationResponse.originalRequest().name()))
                .operation(Operation.ADD)
                .parentReference(EntityReference.mkRef(APPLICATION, appRegistrationResponse.id().get()))
                .userId(username)
                .severity(Severity.INFORMATION)
                .childKind(EntityKind.END_USER_APPLICATION)
                .createdAt(DateTimeUtilities.nowUtc())
                .build();
    }


    private AppRegistrationRequest createAppRegistrationRequest(Long id) {

        EndUserApplication euda = endUserAppDao.getById(id);

        if(euda.isPromoted()){
            return null;
        } else {
            return ImmutableAppRegistrationRequest
                    .builder()
                    .name(euda.name())
                    .applicationKind(ApplicationKind.EUC)
                    .assetCode(String.valueOf(euda.id().get()))
                    .businessCriticality(euda.riskRating())
                    .description(euda.description() + "<br>["+ euda.applicationKind() + "]")
                    .lifecyclePhase(euda.lifecyclePhase())
                    .organisationalUnitId(euda.organisationalUnitId())
                    .overallRating(RagRating.R)
                    .addAliases("")
                    .addTags("")
                    .provenance(euda.provenance())
                    .build();
        }
    }

}
