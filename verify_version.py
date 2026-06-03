import pefile

pe = pefile.PE('/tmp/HMCLauncher_info_test_new.exe')
for rsrc in pe.DIRECTORY_ENTRY_RESOURCE.entries:
    if rsrc.id == pefile.RESOURCE_TYPE['RT_VERSION']:
        for sub in rsrc.directory.entries:
            for lang in sub.directory.entries:
                data = pe.get_data(lang.data.struct.OffsetToData, lang.data.struct.Size)
                text = data.decode('utf-16-le', errors='replace')
                print('VERSIONINFO strings found:')
                keys = ['CompanyName', 'FileDescription', 'FileVersion', 'LegalCopyright', 'OriginalFilename', 'ProductName', 'ProductVersion']
                for s in keys:
                    idx = text.find(s)
                    if idx >= 0:
                        val_start = idx + len(s) + 1
                        val_end = text.find('\x00', val_start)
                        if val_end < 0:
                            val_end = val_start + 100
                        val = text[val_start:val_end]
                        print(f'  {s}: {val}')
                remaining = ['huang', 'HMCL', 'hello minecraft', 'huangyuhui', 'huanghongxun']
                for bad in remaining:
                    if bad.lower() in text.lower():
                        print(f'  WARNING: Found remaining reference: {bad}')
                    else:
                        print(f'  OK: No {bad} reference found')
print('=== Verification complete ===')