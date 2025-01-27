import {
  ChangeDetectionStrategy,
  ChangeDetectorRef,
  Component,
  OnInit,
} from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { Importer, ImporterService } from '../../services/importer.service';
import { ActionSheetController, ToastController } from '@ionic/angular';

@Component({
  selector: 'app-bucket-edit',
  templateUrl: './importer-edit.page.html',
  styleUrls: ['./importer-edit.page.scss'],
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class ImporterEditPage implements OnInit {
  importerId: string;
  importer: Importer;
  private loading: boolean;

  constructor(
    private readonly importerService: ImporterService,
    private readonly actionSheetCtrl: ActionSheetController,
    private readonly toastCtrl: ToastController,
    private readonly router: Router,
    private readonly activatedRoute: ActivatedRoute,
    private readonly changeRef: ChangeDetectorRef
  ) {}

  ngOnInit() {
    this.activatedRoute.params.subscribe((params) => {
      // console.log('params', params);
      this.importerId = params.id;
      this.initImporter(this.importerId);
    });
  }

  async showOptions() {
    const actionSheet = await this.actionSheetCtrl.create({
      buttons: [
        {
          text: 'Delete',
          role: 'destructive',
          handler: () => {
            this.deleteImporter();
          },
        },
        {
          text: 'Cancel',
          role: 'cancel',
          data: {
            action: 'cancel',
          },
        },
      ],
    });

    await actionSheet.present();
    await actionSheet.onDidDismiss();
  }

  private async initImporter(importerId: string) {
    this.loading = true;
    try {
      this.importer = await this.importerService.getImporter({
        importer: {
          id: importerId,
        },
      });
    } finally {
      this.loading = false;
    }
    this.changeRef.detectChanges();
  }

  private async deleteImporter() {
    await this.importerService.deleteImporter(this.importer.id);
    const toast = await this.toastCtrl.create({
      message: 'Deleted',
      duration: 3000,
      color: 'success',
    });

    await toast.present();
    await this.router.navigate(['/']);
  }
}
