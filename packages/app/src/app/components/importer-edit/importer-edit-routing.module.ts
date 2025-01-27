import { NgModule } from '@angular/core';
import { RouterModule, Routes } from '@angular/router';

import { ImporterEditPage } from './importer-edit.page';

const routes: Routes = [
  {
    path: '',
    component: ImporterEditPage,
  },
];

@NgModule({
  imports: [RouterModule.forChild(routes)],
  exports: [RouterModule],
})
export class ImporterEditPageRoutingModule {}
